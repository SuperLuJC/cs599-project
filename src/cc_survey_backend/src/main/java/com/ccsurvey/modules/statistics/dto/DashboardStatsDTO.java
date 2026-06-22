package com.ccsurvey.modules.statistics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 仪表盘统计数据DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsDTO {

    /**
     * 总用户数
     */
    private Long totalUsers;

    /**
     * 今日新增用户
     */
    private Long todayNewUsers;

    /**
     * 总问卷数
     */
    private Long totalSurveys;

    /**
     * 已发布问卷数
     */
    private Long publishedSurveys;

    /**
     * 总提交数
     */
    private Long totalSubmissions;

    /**
     * 今日提交数
     */
    private Long todaySubmissions;

    /**
     * 平均分
     */
    private BigDecimal avgScore;

    /**
     * 最近7天提交趋势
     */
    private List<Map<String, Object>> submissionTrend;

    /**
     * 问卷提交排行
     */
    private List<Map<String, Object>> surveyRanking;

    /**
     * 用户活跃排行
     */
    private List<Map<String, Object>> userRanking;
}