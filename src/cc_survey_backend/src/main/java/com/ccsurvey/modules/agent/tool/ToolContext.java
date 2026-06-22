package com.ccsurvey.modules.agent.tool;

import lombok.Data;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 工具执行上下文
 */
@Data
public class ToolContext {

    /**
     * 当前用户 ID
     */
    private String userId;

    /**
     * 当前会话 ID
     */
    private String conversationId;

    /**
     * Agent 类型: survey, data, log, general
     */
    private String agentType;

    /**
     * 会话上下文数据
     */
    private Map<String, Object> sessionContext;

    /**
     * 获取上下文值
     */
    @SuppressWarnings("unchecked")
    public <T> T getContext(String key) {
        if (sessionContext == null) return null;
        return (T) sessionContext.get(key);
    }

    /**
     * 设置上下文值
     */
    public void setContext(String key, Object value) {
        if (sessionContext == null) {
            sessionContext = new HashMap<>();
        } else {
            // 检查是否为不可修改的 Map，如果是则复制到新的 HashMap
            try {
                sessionContext.put("__test__", null);
                sessionContext.remove("__test__");
            } catch (UnsupportedOperationException e) {
                // 当前 Map 不可修改，创建新的 HashMap
                Map<String, Object> newMap = new HashMap<>(sessionContext);
                sessionContext = newMap;
            }
        }
        sessionContext.put(key, value);
    }
}
