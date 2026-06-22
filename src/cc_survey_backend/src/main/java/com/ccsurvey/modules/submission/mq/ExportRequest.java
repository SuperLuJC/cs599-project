package com.ccsurvey.modules.submission.mq;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 导出请求 (RabbitMQ消息)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExportRequest {

    /**
     * 用户UUID
     */
    private String userId;

    /**
     * 用户邮箱
     */
    private String email;

    /**
     * 答卷UUID列表
     */
    private List<String> answerUuids;

    /**
     * 请求时间戳
     */
    private long timestamp;
}