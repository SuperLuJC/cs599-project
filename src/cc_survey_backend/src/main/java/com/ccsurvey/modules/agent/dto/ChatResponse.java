package com.ccsurvey.modules.agent.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 聊天响应
 */
@Data
public class ChatResponse {

    /**
     * 响应内容
     */
    private String content;

    /**
     * 角色
     */
    private String role;

    /**
     * Tool 调用列表
     */
    private List<ToolCall> toolCalls;

    /**
     * 输入 token 数
     */
    private Integer promptTokens;

    /**
     * 输出 token 数
     */
    private Integer completionTokens;

    /**
     * 总 token 数
     */
    private Integer totalTokens;

    /**
     * 是否有 tool 调用
     */
    public boolean hasToolCalls() {
        return toolCalls != null && !toolCalls.isEmpty();
    }

    /**
     * Tool 调用
     */
    @Data
    public static class ToolCall {
        /**
         * 调用 ID
         */
        private String id;

        /**
         * 类型: function
         */
        private String type;

        /**
         * 函数名
         */
        private String name;

        /**
         * 参数
         */
        private Map<String, Object> arguments;
    }
}
