package com.ccsurvey.modules.agent.agent;

import lombok.Data;

import java.util.Map;

/**
 * Agent 上下文
 * 在 Agent 执行过程中传递的上下文信息
 */
@Data
public class AgentContext {

    /**
     * 会话ID
     */
    private String conversationId;

    /**
     * 用户ID
     */
    private String userId;

    /**
     * Agent 类型
     */
    private String agentType;

    /**
     * 会话上下文数据
     */
    private Map<String, Object> sessionContext;

    /**
     * 意图识别结果
     */
    private com.ccsurvey.modules.agent.intent.IntentResult intentResult;

    /**
     * 最大迭代次数
     */
    private int maxIterations = 5;

    /**
     * 当前迭代次数
     */
    private int currentIteration = 0;

    /**
     * 是否流式输出
     */
    private boolean streaming = false;

    /**
     * 用户消息
     */
    private String userMessage;

    /**
     * 创建上下文
     */
    public static AgentContext of(String conversationId, String userId) {
        AgentContext context = new AgentContext();
        context.setConversationId(conversationId);
        context.setUserId(userId);
        return context;
    }

    /**
     * 创建带用户消息的上下文
     */
    public static AgentContext of(String conversationId, String userId, String userMessage) {
        AgentContext context = new AgentContext();
        context.setConversationId(conversationId);
        context.setUserId(userId);
        context.setUserMessage(userMessage);
        return context;
    }

    /**
     * 获取上下文值
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        if (sessionContext == null) {
            return null;
        }
        return (T) sessionContext.get(key);
    }

    /**
     * 设置上下文值
     */
    public void set(String key, Object value) {
        if (sessionContext == null) {
            sessionContext = new java.util.HashMap<>();
        }
        sessionContext.put(key, value);
    }
}