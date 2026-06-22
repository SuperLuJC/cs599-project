package com.ccsurvey.modules.file.service;

import com.ccsurvey.common.exception.BusinessException;
import com.ccsurvey.common.response.ErrorCode;
import com.ccsurvey.modules.file.entity.FileMetadata;
import com.ccsurvey.modules.file.repository.FileRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 文件服务
 */
@Slf4j
@Service
public class FileService {

    private final FileRepository fileRepository;

    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;

    @Value("${file.temp-dir:./temp}")
    private String tempDir;

    @Value("${file.max-size:20971520}")
    private long maxFileSize;

    // 允许的文件类型
    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp",
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/msword",
            "text/plain"
    );

    // 允许的扩展名
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "gif", "webp",
            "pdf",
            "xlsx", "xls",
            "docx", "doc",
            "txt"
    );

    public FileService(FileRepository fileRepository) {
        this.fileRepository = fileRepository;
    }

    /**
     * 上传文件
     * 文件存储路径: temp/{username}_{surveyTitle}/{uuid}.{ext}
     */
    public Map<String, Object> upload(MultipartFile file, String userId, String username, String surveyTitle) {
        // 验证文件
        validateFile(file);

        try {
            // 生成存储路径
            String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            String uuid = UUID.randomUUID().toString().replace("-", "");
            String extension = FilenameUtils.getExtension(file.getOriginalFilename());
            String storedName = uuid + "." + extension;

            // 构建目录名: 用户名_问卷标题 (确保唯一性)
            String userIdentifier = username != null ? sanitizeFileName(username) : "user_" + (userId != null ? userId : "anonymous");
            String surveyIdentifier = surveyTitle != null ? sanitizeFileName(surveyTitle) : "survey";
            String folderName = userIdentifier + "_" + surveyIdentifier;

            // 临时目录 (提交后移动到正式目录)
            Path tempDirPath = Paths.get(tempDir, folderName);
            Files.createDirectories(tempDirPath);

            Path targetPath = tempDirPath.resolve(storedName);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            // 计算文件哈希
            String fileHash = calculateHash(file.getBytes());

            // 保存文件元数据
            FileMetadata metadata = new FileMetadata();
            metadata.setId(uuid);
            metadata.setOriginalName(file.getOriginalFilename());
            metadata.setStoredName(storedName);
            metadata.setFilePath(targetPath.toString());
            metadata.setFileUrl("/files/" + folderName + "/" + storedName);
            metadata.setFileSize(file.getSize());
            metadata.setFileType(file.getContentType());
            metadata.setFileExt(extension);
            metadata.setFileHash(fileHash);
            metadata.setUploadUserId(userId);
            metadata.setUploadUserName(username);
            metadata.setRelatedType("temp");
            metadata.setStatus(0); // 临时状态

            fileRepository.insert(metadata);

            log.info("文件上传成功: uuid={}, name={}, path={}", uuid, file.getOriginalFilename(), folderName);

            return Map.of(
                    "uuid", uuid,
                    "name", file.getOriginalFilename(),
                    "url", metadata.getFileUrl(),
                    "size", file.getSize()
            );

        } catch (IOException e) {
            log.error("文件上传失败", e);
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    /**
     * 移动临时文件到正式目录
     * 正式目录: uploads/{username}_{surveyTitle}/{uuid}.{ext}
     */
    public String moveToPermanent(String tempUrl, String username, String surveyTitle) {
        if (tempUrl == null) {
            return tempUrl;
        }

        try {
            // 从URL中提取文件夹名 (格式: /files/{username}_{surveyTitle}/{filename})
            String[] parts = tempUrl.split("/");
            if (parts.length < 4) {
                return tempUrl;
            }

            String folderName = parts[2]; // {username}_{surveyTitle}
            String fileName = parts[3];   // {uuid}.{ext}

            // 构建源路径和目标路径
            Path sourcePath = Paths.get(tempDir, folderName, fileName);

            // 目标目录: uploads/{username}_{surveyTitle}/
            String userIdentifier = username != null ? sanitizeFileName(username) : "anonymous";
            String surveyIdentifier = surveyTitle != null ? sanitizeFileName(surveyTitle) : "survey";
            String permFolderName = userIdentifier + "_" + surveyIdentifier;

            Path targetDir = Paths.get(uploadDir, permFolderName);
            Files.createDirectories(targetDir);

            Path targetPath = targetDir.resolve(fileName);

            if (Files.exists(sourcePath)) {
                Files.move(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);

                String permUrl = "/files/" + permFolderName + "/" + fileName;

                // 更新文件元数据
                FileMetadata metadata = fileRepository.selectById(FilenameUtils.getBaseName(fileName));
                if (metadata != null) {
                    metadata.setFilePath(targetPath.toString());
                    metadata.setFileUrl(permUrl);
                    metadata.setStatus(1);
                    fileRepository.updateById(metadata);
                }

                log.info("文件移动到正式目录: {} -> {}", sourcePath, targetPath);
                return permUrl;
            }
        } catch (Exception e) {
            log.error("文件移动失败: {}", tempUrl, e);
        }

        return tempUrl;
    }

    /**
     * 验证文件
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.FILE_EMPTY);
        }

        if (file.getSize() > maxFileSize) {
            throw new BusinessException(ErrorCode.FILE_TOO_LARGE);
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new BusinessException(ErrorCode.FILE_TYPE_NOT_ALLOWED);
        }

        String extension = FilenameUtils.getExtension(file.getOriginalFilename());
        if (extension == null || !ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new BusinessException(ErrorCode.FILE_TYPE_NOT_ALLOWED);
        }

        // 检查文件名是否包含非法字符
        String filename = file.getOriginalFilename();
        if (filename != null && filename.matches(".*[\\\\/:*?\"<>|].*")) {
            throw new BusinessException(ErrorCode.FILE_NAME_INVALID);
        }
    }

    /**
     * 计算文件哈希
     */
    private String calculateHash(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 清理文件名
     */
    private String sanitizeFileName(String name) {
        if (name == null) return "unknown";
        return name.replaceAll("[\\\\/:*?\"<>|\\s]", "_");
    }
}