package com.ccsurvey.modules.agent.agent;

import lombok.Data;

import java.util.Map;

/**
 * Agent 响应
 */
@Data
public class AgentResponse {

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 响应内容
     */
    private String content;

    /**
     * 会话ID
     */
    private String conversationId;

    /**
     * Agent 类型
     */
    private String agentType;

    /**
     * 工具调用记录
     */
    private java.util.List<ToolExecution> toolExecutions;

    /**
     * 额外数据
     */
    private Map<String, Object> metadata;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 创建成功响应
     */
    public static AgentResponse success(String content) {
        AgentResponse response = new AgentResponse();
        response.setSuccess(true);
        response.setContent(content);
        return response;
    }

    /**
     * 创建成功响应（带会话ID）
     */
    public static AgentResponse success(String conversationId, String content) {
        AgentResponse response = new AgentResponse();
        response.setSuccess(true);
        response.setConversationId(conversationId);
        response.setContent(content);
        return response;
    }

    /**
     * 创建失败响应
     */
    public static AgentResponse error(String errorMessage) {
        AgentResponse response = new AgentResponse();
        response.setSuccess(false);
        response.setErrorMessage(errorMessage);
        return response;
    }

    /**
     * 工具执行记录
     */
    @Data
    public static class ToolExecution {
        private String toolName;
        private Map<String, Object> arguments;
        private Object result;
        private boolean success;
        private String error;
    }
}