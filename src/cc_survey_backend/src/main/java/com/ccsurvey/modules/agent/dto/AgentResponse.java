package com.ccsurvey.modules.agent.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Agent 响应
 */
@Data
public class AgentResponse {

    /**
     * 会话ID
     */
    private String conversationId;

    /**
     * 响应内容 (Markdown)
     */
    private String content;

    /**
     * Agent 类型
     */
    private String agentType;

    /**
     * 是否成功
     */
    private boolean success = true;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 工具执行记录
     */
    private List<ToolExecutionRecord> toolExecutions;

    /**
     * 额外元数据
     */
    private Map<String, Object> metadata;

    /**
     * 工具执行记录
     */
    @Data
    public static class ToolExecutionRecord {
        private String toolName;
        private Map<String, Object> arguments;
        private Object result;
        private boolean success;
        private String error;
    }

    /**
     * 创建成功响应
     */
    public static AgentResponse success(String conversationId, String content) {
        AgentResponse response = new AgentResponse();
        response.setConversationId(conversationId);
        response.setContent(content);
        response.setSuccess(true);
        return response;
    }

    /**
     * 创建错误响应
     */
    public static AgentResponse error(String errorMessage) {
        AgentResponse response = new AgentResponse();
        response.setSuccess(false);
        response.setErrorMessage(errorMessage);
        return response;
    }
}
