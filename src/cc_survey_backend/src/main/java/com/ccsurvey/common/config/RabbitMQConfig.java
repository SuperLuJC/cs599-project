package com.ccsurvey.common.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ配置
 */
@Configuration
public class RabbitMQConfig {

    // ==================== 队列名称常量 ====================
    public static final String SUBMISSION_QUEUE = "survey.submission";
    public static final String NOTIFICATION_QUEUE = "survey.notification";
    public static final String EXPORT_QUEUE = "survey.export";
    public static final String EMAIL_QUEUE = "survey.email";

    // ==================== 交换机名称常量 ====================
    public static final String SURVEY_EXCHANGE = "survey.exchange";

    // ==================== 路由键常量 ====================
    public static final String SUBMISSION_ROUTING_KEY = "submission.created";
    public static final String NOTIFICATION_ROUTING_KEY = "notification.send";
    public static final String EXPORT_ROUTING_KEY = "export.request";
    public static final String EMAIL_ROUTING_KEY = "email.send";

    // ==================== 死信队列 ====================
    public static final String DLQ_QUEUE = "survey.dlq";
    public static final String DLQ_EXCHANGE = "survey.dlq.exchange";
    public static final String DLQ_ROUTING_KEY = "dlq";

    @Value("${spring.rabbitmq.listener.simple.prefetch:10}")
    private int prefetch;

    /**
     * JSON消息转换器
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * RabbitTemplate配置
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }

    /**
     * 监听器容器工厂配置
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter());
        factory.setPrefetchCount(prefetch);
        factory.setDefaultRequeueRejected(false);
        return factory;
    }

    // ==================== 交换机定义 ====================

    @Bean
    public DirectExchange surveyExchange() {
        return ExchangeBuilder.directExchange(SURVEY_EXCHANGE)
                .durable(true)
                .build();
    }

    @Bean
    public DirectExchange dlqExchange() {
        return ExchangeBuilder.directExchange(DLQ_EXCHANGE)
                .durable(true)
                .build();
    }

    // ==================== 队列定义 ====================

    /**
     * 提交队列
     */
    @Bean
    public Queue submissionQueue() {
        return QueueBuilder.durable(SUBMISSION_QUEUE)
                .withArgument("x-message-ttl", 86400000) // 24小时TTL
                .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DLQ_ROUTING_KEY)
                .build();
    }

    /**
     * 通知队列
     */
    @Bean
    public Queue notificationQueue() {
        return QueueBuilder.durable(NOTIFICATION_QUEUE)
                .withArgument("x-message-ttl", 604800000) // 7天TTL
                .build();
    }

    /**
     * 导出队列
     */
    @Bean
    public Queue exportQueue() {
        return QueueBuilder.durable(EXPORT_QUEUE)
                .withArgument("x-message-ttl", 3600000) // 1小时TTL
                .build();
    }

    /**
     * 邮件队列
     */
    @Bean
    public Queue emailQueue() {
        return QueueBuilder.durable(EMAIL_QUEUE)
                .withArgument("x-message-ttl", 86400000) // 24小时TTL
                .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DLQ_ROUTING_KEY)
                .build();
    }

    /**
     * 死信队列
     */
    @Bean
    public Queue dlqQueue() {
        return QueueBuilder.durable(DLQ_QUEUE).build();
    }

    // ==================== 绑定定义 ====================

    @Bean
    public Binding submissionBinding() {
        return BindingBuilder.bind(submissionQueue())
                .to(surveyExchange())
                .with(SUBMISSION_ROUTING_KEY);
    }

    @Bean
    public Binding notificationBinding() {
        return BindingBuilder.bind(notificationQueue())
                .to(surveyExchange())
                .with(NOTIFICATION_ROUTING_KEY);
    }

    @Bean
    public Binding exportBinding() {
        return BindingBuilder.bind(exportQueue())
                .to(surveyExchange())
                .with(EXPORT_ROUTING_KEY);
    }

    @Bean
    public Binding emailBinding() {
        return BindingBuilder.bind(emailQueue())
                .to(surveyExchange())
                .with(EMAIL_ROUTING_KEY);
    }

    @Bean
    public Binding dlqBinding() {
        return BindingBuilder.bind(dlqQueue())
                .to(dlqExchange())
                .with(DLQ_ROUTING_KEY);
    }
}