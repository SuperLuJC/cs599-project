package com.ccsurvey.modules.file.controller;

import com.ccsurvey.common.response.ApiResponse;
import com.ccsurvey.modules.file.service.FileService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 文件上传控制器
 */
@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class FileUploadController {

    private final FileService fileService;

    /**
     * 上传文件
     * 文件存储路径: temp/{username}_{surveyTitle}/{uuid}.{ext}
     */
    @PostMapping("/upload")
    public ApiResponse<Map<String, Object>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "surveyTitle", required = false) String surveyTitle,
            HttpServletRequest request) {

        String userId = (String) request.getAttribute("userId");
        String username = (String) request.getAttribute("username");

        if (userId == null) {
            userId = "anonymous";
            username = "anonymous";
        }

        Map<String, Object> result = fileService.upload(file, userId, username, surveyTitle);
        return ApiResponse.success(result);
    }

    /**
     * 批量上传文件
     */
    @PostMapping("/upload/batch")
    public ApiResponse<Map<String, Object>> uploadBatch(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam(value = "surveyTitle", required = false) String surveyTitle,
            HttpServletRequest request) {

        String userId = (String) request.getAttribute("userId");
        String username = (String) request.getAttribute("username");

        if (userId == null) {
            userId = "anonymous";
            username = "anonymous";
        }

        java.util.List<Map<String, Object>> uploadedFiles = new java.util.ArrayList<>();
        for (MultipartFile file : files) {
            try {
                Map<String, Object> result = fileService.upload(file, userId, username, surveyTitle);
                uploadedFiles.add(result);
            } catch (Exception e) {
                log.warn("文件上传失败: {}", file.getOriginalFilename(), e);
            }
        }

        return ApiResponse.success(Map.of("files", uploadedFiles, "count", uploadedFiles.size()));
    }
}