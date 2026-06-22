package com.ccsurvey.modules.agent.memory;

import dev.langchain4j.data.message.ChatMessage;

import java.util.List;
import java.util.Map;

/**
 * Agent 记忆接口
 * 管理对话历史和上下文
 */
public interface AgentMemory {

    /**
     * 添加消息到记忆
     *
     * @param conversationId 会话ID
     * @param message        消息
     */
    void addMessage(String conversationId, ChatMessage message);

    /**
     * 获取会话的所有消息
     *
     * @param conversationId 会话ID
     * @return 消息列表
     */
    List<ChatMessage> getMessages(String conversationId);

    /**
     * 获取最近N条消息
     *
     * @param conversationId 会话ID
     * @param limit          数量限制
     * @return 消息列表
     */
    List<ChatMessage> getRecentMessages(String conversationId, int limit);

    /**
     * 清除会话记忆
     *
     * @param conversationId 会话ID
     */
    void clear(String conversationId);

    /**
     * 设置上下文数据
     *
     * @param conversationId 会话ID
     * @param key            键
     * @param value          值
     */
    void setContext(String conversationId, String key, Object value);

    /**
     * 获取上下文数据
     *
     * @param conversationId 会话ID
     * @param key            键
     * @param <T>            值类型
     * @return 值
     */
    <T> T getContext(String conversationId, String key);

    /**
     * 获取所有上下文数据
     *
     * @param conversationId 会话ID
     * @return 上下文Map
     */
    Map<String, Object> getAllContext(String conversationId);

    /**
     * 清除上下文数据
     *
     * @param conversationId 会话ID
     * @param key            键
     */
    void clearContext(String conversationId, String key);

    /**
     * 检查会话是否存在
     *
     * @param conversationId 会话ID
     * @return 是否存在
     */
    boolean exists(String conversationId);
}
