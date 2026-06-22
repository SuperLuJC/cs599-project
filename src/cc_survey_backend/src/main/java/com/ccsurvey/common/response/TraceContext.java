package com.ccsurvey.common.response;

import java.util.UUID;

/**
 * 追踪上下文 - 用于请求追踪
 */
public class TraceContext {

    private static final ThreadLocal<String> TRACE_ID = new ThreadLocal<>();

    /**
     * 生成并设置追踪ID
     */
    public static String generateTraceId() {
        String traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        TRACE_ID.set(traceId);
        return traceId;
    }

    /**
     * 获取当前追踪ID
     */
    public static String getTraceId() {
        String traceId = TRACE_ID.get();
        return traceId != null ? traceId : "unknown";
    }

    /**
     * 设置追踪ID
     */
    public static void setTraceId(String traceId) {
        TRACE_ID.set(traceId);
    }

    /**
     * 清除追踪ID
     */
    public static void clear() {
        TRACE_ID.remove();
    }
}