package com.ccsurvey.modules.log.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志实体
 */
@Data
@TableName("sys_oper_log")
public class OperationLog {

    /**
     * UUID主键 (32位无横线)
     */
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    private String traceId;

    private String title;

    private String businessType;

    private String method;

    private String requestMethod;

    /**
     * 操作人UUID
     */
    private String operUserId;

    /**
     * 操作人用户名 (展示用)
     */
    private String operName;

    private String operUrl;

    private String operIp;

    private String operLocation;

    private String operParam;

    private String jsonResult;

    private Integer status;

    private String errorMsg;

    private Long costTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime operTime;
}
