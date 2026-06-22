package com.ccsurvey.modules.submission.mq;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 提交事件 (RabbitMQ消息)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmissionEvent {

    /**
     * 答卷UUID
     */
    private String answerUuid;

    /**
     * 问卷模板UUID
     */
    private String templateUuid;

    /**
     * 用户UUID
     */
    private String userId;

    /**
     * 提交人姓名
     */
    private String submitterName;

    /**
     * 总分
     */
    private BigDecimal totalScore;

    /**
     * 提交时间戳
     */
    private long timestamp;

    /**
     * 是否需要通知管理员
     */
    private boolean notifyAdmin;
}