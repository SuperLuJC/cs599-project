package com.ccsurvey.modules.agent.mq;

import com.ccsurvey.modules.agent.intent.Intent;
import lombok.Data;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Agent 任务定义
 * 用于消息队列传递的异步任务
 */
@Data
public class AgentTask {

    /**
     * 任务ID
     */
    private String taskId;

    /**
     * 会话ID
     */
    private String conversationId;

    /**
     * 用户ID
     */
    private String userId;

    /**
     * Agent 类型
     */
    private String agentType;

    /**
     * 识别的意图
     */
    private Intent intent;

    /**
     * 用户消息
     */
    private String message;

    /**
     * 任务上下文
     */
    private Map<String, Object> context;

    /**
     * 任务优先级
     */
    private TaskPriority priority;

    /**
     * 创建时间
     */
    private Instant createdAt;

    /**
     * 超时时间（秒）
     */
    private Long timeoutSeconds;

    /**
     * 是否流式输出
     */
    private boolean streaming;

    /**
     * 任务优先级
     */
    public enum TaskPriority {
        LOW(1),
        NORMAL(5),
        HIGH(8),
        URGENT(10);

        private final int value;

        TaskPriority(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    /**
     * 创建普通任务
     */
    public static AgentTask normal(String userId, String message) {
        AgentTask task = new AgentTask();
        task.setTaskId(java.util.UUID.randomUUID().toString());
        task.setUserId(userId);
        task.setMessage(message);
        task.setPriority(TaskPriority.NORMAL);
        task.setCreatedAt(Instant.now());
        task.setTimeoutSeconds(60L);
        return task;
    }

    /**
     * 创建带会话的任务
     */
    public static AgentTask withConversation(String conversationId, String userId, String message) {
        AgentTask task = normal(userId, message);
        task.setConversationId(conversationId);
        return task;
    }

    /**
     * 创建高优先级任务
     */
    public static AgentTask urgent(String userId, String message) {
        AgentTask task = normal(userId, message);
        task.setPriority(TaskPriority.URGENT);
        return task;
    }
}