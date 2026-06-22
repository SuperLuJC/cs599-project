package com.ccsurvey.modules.submission.mq;

import com.ccsurvey.common.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * 提交事件生产者 (RabbitMQ)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SubmissionProducer {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 发送提交事件
     */
    public void sendSubmissionEvent(SubmissionEvent event) {
        log.info("发送提交事件: answerUuid={}, templateUuid={}", event.getAnswerUuid(), event.getTemplateUuid());
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.SURVEY_EXCHANGE,
                RabbitMQConfig.SUBMISSION_ROUTING_KEY,
                event
        );
    }

    /**
     * 发送导出请求
     */
    public void sendExportRequest(ExportRequest request) {
        log.info("发送导出请求: userId={}, answerCount={}", request.getUserId(), request.getAnswerUuids().size());
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.SURVEY_EXCHANGE,
                RabbitMQConfig.EXPORT_ROUTING_KEY,
                request
        );
    }
}