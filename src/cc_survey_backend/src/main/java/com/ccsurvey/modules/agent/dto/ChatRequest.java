package com.ccsurvey.modules.agent.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 聊天请求
 */
@Data
public class ChatRequest {

    /**
     * 消息列表
     */
    private List<ChatMessage> messages = new ArrayList<>();

    /**
     * 可用的工具列表
     */
    private List<ToolDefinition> tools;

    /**
     * 添加消息
     */
    public ChatRequest addMessage(ChatMessage message) {
        this.messages.add(message);
        return this;
    }

    /**
     * 添加系统消息
     */
    public ChatRequest system(String content) {
        return addMessage(ChatMessage.system(content));
    }

    /**
     * 添加用户消息
     */
    public ChatRequest user(String content) {
        return addMessage(ChatMessage.user(content));
    }

    /**
     * 添加助手消息
     */
    public ChatRequest assistant(String content) {
        return addMessage(ChatMessage.assistant(content));
    }
}
