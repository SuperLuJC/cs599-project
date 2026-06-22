package com.ccsurvey.modules.agent.mq;

import lombok.Data;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Agent 任务结果
 */
@Data
public class AgentTaskResult {

    /**
     * 任务ID
     */
    private String taskId;

    /**
     * 会话ID
     */
    private String conversationId;

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 响应内容
     */
    private String content;

    /**
     * 工具执行记录
     */
    private List<ToolExecution> toolExecutions;

    /**
     * 额外元数据
     */
    private Map<String, Object> metadata;

    /**
     * 执行耗时
     */
    private Duration executionTime;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 完成时间
     */
    private Instant completedAt;

    /**
     * 工具执行记录
     */
    @Data
    public static class ToolExecution {
        private String toolName;
        private Map<String, Object> arguments;
        private Object result;
        private Duration executionTime;
        private boolean success;
        private String error;
    }

    /**
     * 创建成功结果
     */
    public static AgentTaskResult success(String taskId, String conversationId, String content) {
        AgentTaskResult result = new AgentTaskResult();
        result.setTaskId(taskId);
        result.setConversationId(conversationId);
        result.setSuccess(true);
        result.setContent(content);
        result.setCompletedAt(Instant.now());
        return result;
    }

    /**
     * 创建失败结果
     */
    public static AgentTaskResult failure(String taskId, String conversationId, String errorMessage) {
        AgentTaskResult result = new AgentTaskResult();
        result.setTaskId(taskId);
        result.setConversationId(conversationId);
        result.setSuccess(false);
        result.setErrorMessage(errorMessage);
        result.setCompletedAt(Instant.now());
        return result;
    }
}