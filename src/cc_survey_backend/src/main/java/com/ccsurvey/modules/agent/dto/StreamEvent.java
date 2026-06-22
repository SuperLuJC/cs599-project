package com.ccsurvey.modules.agent.dto;

import lombok.Data;

/**
 * 流式响应事件
 */
@Data
public class StreamEvent {

    /**
     * 事件类型
     */
    private String type;

    /**
     * 事件数据
     */
    private Object data;

    /**
     * 会话ID
     */
    private String conversationId;

    /**
     * 事件类型常量
     */
    public static final String TYPE_CONVERSATION_CREATED = "conversation_created";
    public static final String TYPE_TITLE = "title";
    public static final String TYPE_CONTENT = "content";
    public static final String TYPE_DONE = "done";
    public static final String TYPE_ERROR = "error";

    public static StreamEvent conversationCreated(String conversationId) {
        StreamEvent event = new StreamEvent();
        event.setType(TYPE_CONVERSATION_CREATED);
        event.setConversationId(conversationId);
        return event;
    }

    public static StreamEvent title(String title) {
        StreamEvent event = new StreamEvent();
        event.setType(TYPE_TITLE);
        event.setData(title);
        return event;
    }

    public static StreamEvent content(String content) {
        StreamEvent event = new StreamEvent();
        event.setType(TYPE_CONTENT);
        event.setData(content);
        return event;
    }

    public static StreamEvent done() {
        StreamEvent event = new StreamEvent();
        event.setType(TYPE_DONE);
        return event;
    }

    public static StreamEvent error(String message) {
        StreamEvent event = new StreamEvent();
        event.setType(TYPE_ERROR);
        event.setData(message);
        return event;
    }
}
