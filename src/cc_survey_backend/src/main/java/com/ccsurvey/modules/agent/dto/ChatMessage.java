package com.ccsurvey.modules.agent.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 聊天消息
 */
@Data
public class ChatMessage {

    /**
     * 角色: system, user, assistant, tool
     */
    private String role;

    /**
     * 消息内容
     */
    private String content;

    /**
     * Tool 调用 (仅 assistant 消息)
     */
    private List<Map<String, Object>> toolCalls;

    /**
     * Tool 调用 ID (仅 tool 消息)
     */
    private String toolCallId;

    /**
     * Tool 名称 (仅 tool 消息)
     */
    private String name;

    public static ChatMessage system(String content) {
        ChatMessage msg = new ChatMessage();
        msg.setRole("system");
        msg.setContent(content);
        return msg;
    }

    public static ChatMessage user(String content) {
        ChatMessage msg = new ChatMessage();
        msg.setRole("user");
        msg.setContent(content);
        return msg;
    }

    public static ChatMessage assistant(String content) {
        ChatMessage msg = new ChatMessage();
        msg.setRole("assistant");
        msg.setContent(content);
        return msg;
    }

    public static ChatMessage tool(String toolCallId, String name, String content) {
        ChatMessage msg = new ChatMessage();
        msg.setRole("tool");
        msg.setToolCallId(toolCallId);
        msg.setName(name);
        msg.setContent(content);
        return msg;
    }
}
