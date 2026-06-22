package com.ccsurvey.modules.agent.mq;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Agent 消息队列配置
 * 注意：基础的 MessageConverter 和 RabbitTemplate 在 RabbitMQConfig 中已定义
 */
@Configuration
public class AgentMqConfig {

    /**
     * 队列名称常量
     */
    public static final String TASK_QUEUE = "agent.task.queue";
    public static final String RESULT_QUEUE = "agent.result.queue";
    public static final String DLQ = "agent.dlq";
    public static final String DELAY_QUEUE = "agent.task.delayed.queue";

    /**
     * 交换机名称
     */
    public static final String EXCHANGE = "agent.exchange";
    public static final String DLQ_EXCHANGE = "agent.dlq.exchange";

    /**
     * 路由键
     */
    public static final String TASK_ROUTING_KEY = "agent.task";
    public static final String RESULT_ROUTING_KEY = "agent.result";
    public static final String DELAYED_ROUTING_KEY = "agent.task.delayed";

    /**
     * Agent 交换机
     */
    @Bean
    public DirectExchange agentExchange() {
        return new DirectExchange(EXCHANGE, true, false);
    }

    /**
     * 死信交换机
     */
    @Bean
    public DirectExchange agentDlqExchange() {
        return new DirectExchange(DLQ_EXCHANGE, true, false);
    }

    /**
     * 任务队列（支持优先级）
     */
    @Bean
    public Queue agentTaskQueue() {
        return QueueBuilder.durable(TASK_QUEUE)
                .withArgument("x-max-priority", 10)
                .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", "agent.dlq")
                .build();
    }

    /**
     * 结果队列
     */
    @Bean
    public Queue agentResultQueue() {
        return QueueBuilder.durable(RESULT_QUEUE).build();
    }

    /**
     * 死信队列
     */
    @Bean
    public Queue agentDlq() {
        return QueueBuilder.durable(DLQ).build();
    }

    /**
     * 延迟队列（用于重试）
     */
    @Bean
    public Queue agentDelayQueue() {
        return QueueBuilder.durable(DELAY_QUEUE)
                .withArgument("x-dead-letter-exchange", EXCHANGE)
                .withArgument("x-dead-letter-routing-key", TASK_ROUTING_KEY)
                .build();
    }

    /**
     * 任务队列绑定
     */
    @Bean
    public Binding agentTaskBinding() {
        return BindingBuilder.bind(agentTaskQueue())
                .to(agentExchange())
                .with(TASK_ROUTING_KEY);
    }

    /**
     * 结果队列绑定
     */
    @Bean
    public Binding agentResultBinding() {
        return BindingBuilder.bind(agentResultQueue())
                .to(agentExchange())
                .with(RESULT_ROUTING_KEY);
    }

    /**
     * 死信队列绑定
     */
    @Bean
    public Binding agentDlqBinding() {
        return BindingBuilder.bind(agentDlq())
                .to(agentDlqExchange())
                .with("agent.dlq");
    }

    /**
     * 延迟队列绑定
     */
    @Bean
    public Binding agentDelayBinding() {
        return BindingBuilder.bind(agentDelayQueue())
                .to(agentExchange())
                .with(DELAYED_ROUTING_KEY);
    }
}
