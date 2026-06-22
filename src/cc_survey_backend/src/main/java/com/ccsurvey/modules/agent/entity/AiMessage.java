package com.ccsurvey.modules.agent.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * AI 消息实体
 */
@Data
@TableName(value = "ai_message", autoResultMap = true)
public class AiMessage {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /**
     * 消息序号（用于排序）
     */
    private Integer seq;

    /**
     * 会话ID
     */
    private String conversationId;

    /**
     * 消息角色: user, assistant, tool
     */
    private String role;

    /**
     * 消息内容 (Markdown)
     */
    private String content;

    /**
     * Tool 调用记录
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<ToolCall> toolCalls;

    /**
     * Tool 返回结果
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<ToolResult> toolResults;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * Tool 调用记录
     */
    @Data
    public static class ToolCall {
        private String id;
        private String name;
        private Map<String, Object> arguments;
    }

    /**
     * Tool 返回结果
     */
    @Data
    public static class ToolResult {
        private String toolCallId;
        private String name;
        private Object result;
        private Boolean isError;
    }
}