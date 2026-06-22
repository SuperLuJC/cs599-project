package com.ccsurvey.modules.file.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文件元数据实体
 */
@Data
@TableName("sys_file")
public class FileMetadata {

    /**
     * UUID主键 (32位无横线)
     */
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    private String originalName;

    private String storedName;

    private String filePath;

    private String fileUrl;

    private Long fileSize;

    private String fileType;

    private String fileExt;

    private String fileHash;

    /**
     * 上传用户UUID
     */
    private String uploadUserId;

    /**
     * 上传用户名 (展示用)
     */
    private String uploadUserName;

    private String relatedType;

    private String relatedId;

    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
