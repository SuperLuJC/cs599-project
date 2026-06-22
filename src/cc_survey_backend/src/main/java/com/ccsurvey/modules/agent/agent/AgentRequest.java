package com.ccsurvey.modules.agent.agent;

import lombok.Data;

import java.util.Map;

/**
 * Agent 请求
 */
@Data
public class AgentRequest {

    /**
     * 会话ID
     */
    private String conversationId;

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 用户消息
     */
    private String message;

    /**
     * Agent 类型
     */
    private String agentType;

    /**
     * 意图识别结果
     */
    private com.ccsurvey.modules.agent.intent.IntentResult intentResult;

    /**
     * 额外参数
     */
    private Map<String, Object> parameters;

    /**
     * 创建简单请求
     */
    public static AgentRequest of(String userId, String message) {
        AgentRequest request = new AgentRequest();
        request.setUserId(userId);
        request.setMessage(message);
        return request;
    }

    /**
     * 创建带会话的请求
     */
    public static AgentRequest of(String conversationId, String userId, String message) {
        AgentRequest request = new AgentRequest();
        request.setConversationId(conversationId);
        request.setUserId(userId);
        request.setMessage(message);
        return request;
    }
}