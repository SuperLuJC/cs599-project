package com.ccsurvey.modules.submission.mq;

import com.ccsurvey.common.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 提交事件消费者 (RabbitMQ)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SubmissionConsumer {

    // private final StatisticsService statisticsService;
    // private final NotificationService notificationService;

    /**
     * 处理提交事件
     */
    @RabbitListener(queues = RabbitMQConfig.SUBMISSION_QUEUE)
    public void handleSubmission(SubmissionEvent event) {
        log.info("处理提交事件: answerUuid={}", event.getAnswerUuid());

        try {
            // 1. 更新统计数据 (异步)
            // statisticsService.updateSubmissionStats(event.getTemplateUuid());

            // 2. 更新排行榜 (如果有分数)
            if (event.getTotalScore() != null) {
                // statisticsService.updateRankings(event);
                log.info("更新排行榜: userId={}, score={}", event.getUserId(), event.getTotalScore());
            }

            // 3. 发送通知 (如果需要)
            if (event.isNotifyAdmin()) {
                // notificationService.sendAdminNotification(event);
                log.info("发送管理员通知: templateUuid={}", event.getTemplateUuid());
            }

            log.info("提交事件处理完成: answerUuid={}", event.getAnswerUuid());

        } catch (Exception e) {
            log.error("处理提交事件失败: answerUuid={}", event.getAnswerUuid(), e);
            // 抛出异常会触发重试机制
            throw e;
        }
    }

    /**
     * 处理导出请求
     */
    @RabbitListener(queues = RabbitMQConfig.EXPORT_QUEUE)
    public void handleExportRequest(ExportRequest request) {
        log.info("处理导出请求: userId={}, answerCount={}", request.getUserId(), request.getAnswerUuids().size());

        try {
            // 1. 生成ZIP文件
            // byte[] zipData = exportService.generateZip(request.getAnswerUuids());

            // 2. 存储到临时位置
            // String downloadUrl = fileStorageService.storeTempFile(zipData, request.getUserId());

            // 3. 发送通知
            // notificationService.notifyExportComplete(request.getEmail(), downloadUrl);

            log.info("导出请求处理完成: userId={}", request.getUserId());

        } catch (Exception e) {
            log.error("处理导出请求失败: userId={}", request.getUserId(), e);
            throw e;
        }
    }
}