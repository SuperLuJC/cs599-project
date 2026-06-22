package com.ccsurvey.common.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 统一API响应封装
 *
 * @param <T> 数据类型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {

    /**
     * 响应码
     */
    private int code;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 响应数据
     */
    private T data;

    /**
     * 响应时间戳
     */
    private long timestamp;

    /**
     * 请求追踪ID
     */
    private String traceId;

    /**
     * 成功响应 (无消息)
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(200, "success", data, Instant.now().toEpochMilli(), TraceContext.getTraceId());
    }

    /**
     * 成功响应 (自定义消息)
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(200, message, data, Instant.now().toEpochMilli(), TraceContext.getTraceId());
    }

    /**
     * 成功响应 (无数据)
     */
    public static <T> ApiResponse<T> success(String message) {
        return new ApiResponse<>(200, message, null, Instant.now().toEpochMilli(), TraceContext.getTraceId());
    }

    /**
     * 错误响应 (使用错误码)
     */
    public static <T> ApiResponse<T> error(ErrorCode errorCode) {
        return new ApiResponse<>(errorCode.getCode(), errorCode.getMessage(), null, Instant.now().toEpochMilli(), TraceContext.getTraceId());
    }

    /**
     * 错误响应 (自定义消息)
     */
    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(code, message, null, Instant.now().toEpochMilli(), TraceContext.getTraceId());
    }

    /**
     * 错误响应 (错误码 + 自定义消息)
     */
    public static <T> ApiResponse<T> error(ErrorCode errorCode, String message) {
        return new ApiResponse<>(errorCode.getCode(), message, null, Instant.now().toEpochMilli(), TraceContext.getTraceId());
    }
}