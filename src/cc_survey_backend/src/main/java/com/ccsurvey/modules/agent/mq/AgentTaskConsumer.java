package com.ccsurvey.modules.agent.mq;

import com.ccsurvey.modules.agent.agent.AgentContext;
import com.ccsurvey.modules.agent.agent.AgentRequest;
import com.ccsurvey.modules.agent.agent.AgentResponse;
import com.ccsurvey.modules.agent.orchestrator.AgentOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Agent 任务消费者
 * 负责从消息队列消费任务并执行
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AgentTaskConsumer {

    private final AgentOrchestrator orchestrator;
    private final AgentTaskProducer taskProducer;

    /**
     * 最大重试次数
     */
    private static final int MAX_RETRIES = 3;

    /**
     * 重试计数器（内存中，重启后丢失）
     */
    private final Map<String, Integer> retryCount = new ConcurrentHashMap<>();

    /**
     * 消费 Agent 任务
     */
    @RabbitListener(queues = AgentMqConfig.TASK_QUEUE)
    public void consumeTask(AgentTask task) {
        String taskId = task.getTaskId();
        Instant startTime = Instant.now();

        log.info("Consuming agent task: taskId={}, conversationId={}", taskId, task.getConversationId());

        try {
            // 执行任务
            AgentTaskResult result = executeTask(task, startTime);

            // 发送结果
            taskProducer.sendResult(result);

            // 清理重试计数
            retryCount.remove(taskId);

            log.info("Agent task completed: taskId={}, success={}, duration={}ms",
                    taskId, result.isSuccess(),
                    Duration.between(startTime, Instant.now()).toMillis());

        } catch (Exception e) {
            log.error("Failed to process agent task: taskId={}, error={}", taskId, e.getMessage(), e);

            // 处理失败
            handleFailure(task, taskId, e);
        }
    }

    /**
     * 执行 Agent 任务
     */
    private AgentTaskResult executeTask(AgentTask task, Instant startTime) {
        try {
            // 构建 Agent 请求
            AgentRequest request = new AgentRequest();
            request.setConversationId(task.getConversationId());
            request.setUserId(task.getUserId());
            request.setMessage(task.getMessage());
            request.setAgentType(task.getAgentType());

            // 构建上下文
            AgentContext context = AgentContext.of(task.getConversationId(), task.getUserId());
            if (task.getContext() != null) {
                context.getSessionContext().putAll(task.getContext());
            }

            // 执行编排
            AgentResponse response = orchestrator.execute(request);

            // 构建结果
            AgentTaskResult result = new AgentTaskResult();
            result.setTaskId(task.getTaskId());
            result.setConversationId(task.getConversationId());
            result.setSuccess(response.isSuccess());
            result.setContent(response.getContent());
            result.setExecutionTime(Duration.between(startTime, Instant.now()));
            result.setCompletedAt(Instant.now());

            if (!response.isSuccess()) {
                result.setErrorMessage(response.getErrorMessage());
            }

            if (response.getMetadata() != null) {
                result.setMetadata(response.getMetadata());
            }

            return result;

        } catch (Exception e) {
            log.error("Error executing agent task: {}", e.getMessage(), e);

            return AgentTaskResult.failure(
                    task.getTaskId(),
                    task.getConversationId(),
                    e.getMessage()
            );
        }
    }

    /**
     * 处理任务失败
     */
    private void handleFailure(AgentTask task, String taskId, Exception error) {
        int retries = retryCount.getOrDefault(taskId, 0) + 1;
        retryCount.put(taskId, retries);

        if (retries < MAX_RETRIES) {
            // 重试：发送到延迟队列
            long delayMs = calculateBackoffDelay(retries);
            taskProducer.sendDelayedTask(task, delayMs);

            log.info("Task scheduled for retry: taskId={}, retry={}, delayMs={}",
                    taskId, retries, delayMs);
        } else {
            // 超过最大重试次数，发送到死信队列
            taskProducer.sendToDLQ(task, error.getMessage());

            // 发送失败结果
            AgentTaskResult result = AgentTaskResult.failure(
                    taskId,
                    task.getConversationId(),
                    "Max retries exceeded: " + error.getMessage()
            );
            taskProducer.sendResult(result);

            // 清理重试计数
            retryCount.remove(taskId);

            log.warn("Task moved to DLQ after {} retries: taskId={}", MAX_RETRIES, taskId);
        }
    }

    /**
     * 计算退避延迟（指数退避）
     */
    private long calculateBackoffDelay(int retryCount) {
        // 1s, 5s, 25s
        return (long) (1000 * Math.pow(5, retryCount - 1));
    }

    /**
     * 消费死信队列消息（人工处理或告警）
     */
    @RabbitListener(queues = AgentMqConfig.DLQ)
    public void consumeDLQ(AgentTask task) {
        log.error("Task in DLQ: taskId={}, agentType={}", task.getTaskId(), task.getAgentType());

        // TODO: 发送告警通知、记录到数据库或触发人工处理流程
        // 这里可以集成告警系统（如邮件、钉钉、Slack等）
    }
}