package com.ccsurvey.modules.agent.protocol;

import lombok.Data;

import java.time.Instant;
import java.util.Map;

/**
 * Agent 间消息
 * 用于多 Agent 协作时的通信
 */
@Data
public class AgentMessage {

    /**
     * 消息ID
     */
    private String messageId;

    /**
     * 消息类型
     */
    private MessageType type;

    /**
     * 源 Agent
     */
    private String sourceAgent;

    /**
     * 目标 Agent
     */
    private String targetAgent;

    /**
     * 会话ID
     */
    private String conversationId;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 附加数据
     */
    private Map<String, Object> payload;

    /**
     * 时间戳
     */
    private Instant timestamp;

    /**
     * 消息类型枚举
     */
    public enum MessageType {
        /**
         * 任务请求
         */
        TASK_REQUEST,

        /**
         * 任务结果
         */
        TASK_RESULT,

        /**
         * 数据查询
         */
        DATA_QUERY,

        /**
         * 数据响应
         */
        DATA_RESPONSE,

        /**
         * 状态更新
         */
        STATUS_UPDATE,

        /**
         * 错误报告
         */
        ERROR
    }

    /**
     * 创建任务请求消息
     */
    public static AgentMessage taskRequest(String sourceAgent, String targetAgent,
                                            String conversationId, String content) {
        AgentMessage message = new AgentMessage();
        message.setMessageId(java.util.UUID.randomUUID().toString());
        message.setType(MessageType.TASK_REQUEST);
        message.setSourceAgent(sourceAgent);
        message.setTargetAgent(targetAgent);
        message.setConversationId(conversationId);
        message.setContent(content);
        message.setTimestamp(Instant.now());
        return message;
    }

    /**
     * 创建任务结果消息
     */
    public static AgentMessage taskResult(String sourceAgent, String targetAgent,
                                           String conversationId, String content,
                                           Map<String, Object> payload) {
        AgentMessage message = new AgentMessage();
        message.setMessageId(java.util.UUID.randomUUID().toString());
        message.setType(MessageType.TASK_RESULT);
        message.setSourceAgent(sourceAgent);
        message.setTargetAgent(targetAgent);
        message.setConversationId(conversationId);
        message.setContent(content);
        message.setPayload(payload);
        message.setTimestamp(Instant.now());
        return message;
    }

    /**
     * 创建错误消息
     */
    public static AgentMessage error(String sourceAgent, String targetAgent,
                                      String conversationId, String errorMessage) {
        AgentMessage message = new AgentMessage();
        message.setMessageId(java.util.UUID.randomUUID().toString());
        message.setType(MessageType.ERROR);
        message.setSourceAgent(sourceAgent);
        message.setTargetAgent(targetAgent);
        message.setConversationId(conversationId);
        message.setContent(errorMessage);
        message.setTimestamp(Instant.now());
        return message;
    }
}