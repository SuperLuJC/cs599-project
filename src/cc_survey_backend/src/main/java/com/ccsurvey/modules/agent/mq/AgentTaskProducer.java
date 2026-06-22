package com.ccsurvey.modules.agent.mq;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Agent 任务生产者
 * 负责将任务发送到消息队列
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AgentTaskProducer {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 发送任务到队列
     *
     * @param task Agent 任务
     * @return 任务ID
     */
    public String sendTask(AgentTask task) {
        try {
            String taskId = task.getTaskId();

            rabbitTemplate.convertAndSend(
                    AgentMqConfig.EXCHANGE,
                    AgentMqConfig.TASK_ROUTING_KEY,
                    task,
                    message -> {
                        message.getMessageProperties().setMessageId(taskId);
                        message.getMessageProperties().setCorrelationId(taskId);
                        message.getMessageProperties().setHeader("agentType", task.getAgentType());
                        message.getMessageProperties().setHeader("userId", task.getUserId());
                        message.getMessageProperties().setHeader("conversationId", task.getConversationId());
                        message.getMessageProperties().setPriority(task.getPriority().getValue());
                        return message;
                    }
            );

            log.info("Sent agent task: taskId={}, agentType={}, priority={}",
                    taskId, task.getAgentType(), task.getPriority());

            return taskId;
        } catch (Exception e) {
            log.error("Failed to send agent task: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to send agent task", e);
        }
    }

    /**
     * 发送任务结果到队列
     *
     * @param result 任务结果
     */
    public void sendResult(AgentTaskResult result) {
        try {
            String taskId = result.getTaskId();

            rabbitTemplate.convertAndSend(
                    AgentMqConfig.EXCHANGE,
                    AgentMqConfig.RESULT_ROUTING_KEY,
                    result,
                    message -> {
                        message.getMessageProperties().setMessageId(taskId);
                        message.getMessageProperties().setCorrelationId(result.getConversationId());
                        message.getMessageProperties().setHeader("success", result.isSuccess());
                        return message;
                    }
            );

            log.info("Sent agent task result: taskId={}, success={}", taskId, result.isSuccess());
        } catch (Exception e) {
            log.error("Failed to send agent task result: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to send agent task result", e);
        }
    }

    /**
     * 发送任务到延迟队列（用于重试）
     *
     * @param task        Agent 任务
     * @param delayMillis 延迟时间（毫秒）
     */
    public void sendDelayedTask(AgentTask task, long delayMillis) {
        try {
            String taskId = task.getTaskId();

            rabbitTemplate.convertAndSend(
                    AgentMqConfig.EXCHANGE,
                    AgentMqConfig.DELAYED_ROUTING_KEY,
                    task,
                    message -> {
                        message.getMessageProperties().setMessageId(taskId);
                        message.getMessageProperties().setCorrelationId(taskId);
                        message.getMessageProperties().setHeader("x-delay", delayMillis);
                        return message;
                    }
            );

            log.info("Sent delayed agent task: taskId={}, delayMs={}", taskId, delayMillis);
        } catch (Exception e) {
            log.error("Failed to send delayed agent task: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to send delayed agent task", e);
        }
    }

    /**
     * 发送任务到死信队列
     *
     * @param task   Agent 任务
     * @param reason 死信原因
     */
    public void sendToDLQ(AgentTask task, String reason) {
        try {
            String taskId = task.getTaskId();

            rabbitTemplate.convertAndSend(
                    AgentMqConfig.DLQ_EXCHANGE,
                    "agent.dlq",
                    task,
                    message -> {
                        message.getMessageProperties().setMessageId(taskId);
                        message.getMessageProperties().setCorrelationId(taskId);
                        message.getMessageProperties().setHeader("dlqReason", reason);
                        message.getMessageProperties().setHeader("originalAgentType", task.getAgentType());
                        return message;
                    }
            );

            log.warn("Sent task to DLQ: taskId={}, reason={}", taskId, reason);
        } catch (Exception e) {
            log.error("Failed to send task to DLQ: {}", e.getMessage(), e);
        }
    }
}