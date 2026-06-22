package com.ccsurvey.modules.agent.tool;

import lombok.Data;

/**
 * 工具执行结果
 */
@Data
public class ToolResult {

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 结果数据
     */
    private Object data;

    /**
     * 错误信息
     */
    private String error;

    /**
     * 是否需要用户确认
     */
    private boolean requiresConfirmation;

    /**
     * 确认提示信息
     */
    private String confirmationMessage;

    public static ToolResult success(Object data) {
        ToolResult result = new ToolResult();
        result.setSuccess(true);
        result.setData(data);
        return result;
    }

    public static ToolResult error(String error) {
        ToolResult result = new ToolResult();
        result.setSuccess(false);
        result.setError(error);
        return result;
    }

    public static ToolResult needsConfirmation(String message, Object data) {
        ToolResult result = new ToolResult();
        result.setSuccess(true);
        result.setData(data);
        result.setRequiresConfirmation(true);
        result.setConfirmationMessage(message);
        return result;
    }
}
