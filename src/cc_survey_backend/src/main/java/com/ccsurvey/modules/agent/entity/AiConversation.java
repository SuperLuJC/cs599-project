package com.ccsurvey.modules.agent.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 会话实体
 */
@Data
@TableName("ai_conversation")
public class AiConversation {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /**
     * 创建用户ID
     */
    private String userId;

    /**
     * 会话标题
     */
    private String title;

    /**
     * Agent类型: survey, data, log, general
     */
    private String agentType;

    /**
     * 会话状态: 1-活跃, 0-归档
     */
    private Integer status;

    /**
     * 会话上下文 (JSON)
     * 存储当前操作的问卷ID、分析的数据范围等
     */
    @TableField(typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    private java.util.Map<String, Object> context;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}