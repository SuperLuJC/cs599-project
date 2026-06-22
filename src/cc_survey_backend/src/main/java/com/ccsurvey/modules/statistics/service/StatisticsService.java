package com.ccsurvey.modules.statistics.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ccsurvey.modules.statistics.dto.DashboardStatsDTO;
import com.ccsurvey.modules.survey.entity.SurveyAnswer;
import com.ccsurvey.modules.survey.entity.SurveyTemplate;
import com.ccsurvey.modules.survey.repository.SurveyAnswerRepository;
import com.ccsurvey.modules.survey.repository.SurveyTemplateRepository;
import com.ccsurvey.modules.user.entity.User;
import com.ccsurvey.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 统计服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final UserRepository userRepository;
    private final SurveyTemplateRepository templateRepository;
    private final SurveyAnswerRepository answerRepository;

    /**
     * 获取仪表盘统计数据
     */
    public DashboardStatsDTO getDashboardStats() {
        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime todayEnd = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);

        // 用户统计
        long totalUsers = userRepository.selectCount(null);
        long todayNewUsers = userRepository.selectCount(
                new LambdaQueryWrapper<User>()
                        .ge(User::getCreateTime, todayStart)
        );

        // 问卷统计
        long totalSurveys = templateRepository.selectCount(null);
        long publishedSurveys = templateRepository.selectCount(
                new LambdaQueryWrapper<SurveyTemplate>()
                        .eq(SurveyTemplate::getStatus, 1)
        );

        // 提交统计
        long totalSubmissions = answerRepository.selectCount(null);
        long todaySubmissions = answerRepository.selectCount(
                new LambdaQueryWrapper<SurveyAnswer>()
                        .ge(SurveyAnswer::getCreateTime, todayStart)
        );

        // 平均分
        BigDecimal avgScore = calculateAvgScore();

        // 最近7天提交趋势
        List<Map<String, Object>> submissionTrend = getSubmissionTrend(7);

        // 问卷提交排行
        List<Map<String, Object>> surveyRanking = getSurveyRanking(5);

        // 用户活跃排行
        List<Map<String, Object>> userRanking = getUserRanking(5);

        return DashboardStatsDTO.builder()
                .totalUsers(totalUsers)
                .todayNewUsers(todayNewUsers)
                .totalSurveys(totalSurveys)
                .publishedSurveys(publishedSurveys)
                .totalSubmissions(totalSubmissions)
                .todaySubmissions(todaySubmissions)
                .avgScore(avgScore)
                .submissionTrend(submissionTrend)
                .surveyRanking(surveyRanking)
                .userRanking(userRanking)
                .build();
    }

    /**
     * 计算平均分
     */
    private BigDecimal calculateAvgScore() {
        List<SurveyAnswer> answers = answerRepository.selectList(null);

        if (answers.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal totalScore = answers.stream()
                .filter(a -> a.getTotalScore() != null)
                .map(SurveyAnswer::getTotalScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long count = answers.stream()
                .filter(a -> a.getTotalScore() != null)
                .count();

        if (count == 0) {
            return BigDecimal.ZERO;
        }

        return totalScore.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
    }

    /**
     * 获取提交趋势
     */
    private List<Map<String, Object>> getSubmissionTrend(int days) {
        List<Map<String, Object>> trend = new ArrayList<>();

        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            LocalDateTime start = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime end = LocalDateTime.of(date, LocalTime.MAX);

            long count = answerRepository.selectCount(
                    new LambdaQueryWrapper<SurveyAnswer>()
                            .between(SurveyAnswer::getCreateTime, start, end)
            );

            trend.add(Map.of(
                    "date", date.toString(),
                    "count", count
            ));
        }

        return trend;
    }

    /**
     * 获取问卷提交排行
     */
    private List<Map<String, Object>> getSurveyRanking(int limit) {
        List<SurveyTemplate> templates = templateRepository.selectList(
                new LambdaQueryWrapper<SurveyTemplate>()
                        .eq(SurveyTemplate::getStatus, 1)
        );

        return templates.stream()
                .map(t -> {
                    long count = templateRepository.countSubmissions(t.getId());
                    return Map.<String, Object>of(
                            "uuid", t.getId(),
                            "title", t.getTitle(),
                            "count", count
                    );
                })
                .sorted((a, b) -> Long.compare((Long) b.get("count"), (Long) a.get("count")))
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * 获取用户活跃排行
     */
    private List<Map<String, Object>> getUserRanking(int limit) {
        List<SurveyAnswer> answers = answerRepository.selectList(null);

        Map<String, Long> userSubmissionCount = answers.stream()
                .filter(a -> a.getUserId() != null)
                .collect(Collectors.groupingBy(SurveyAnswer::getUserId, Collectors.counting()));

        return userSubmissionCount.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(limit)
                .map(e -> {
                    User user = userRepository.selectById(e.getKey());
                    return Map.<String, Object>of(
                            "uuid", user != null ? user.getId() : "unknown",
                            "name", user != null ? user.getName() : "未知用户",
                            "count", e.getValue()
                    );
                })
                .collect(Collectors.toList());
    }

    /**
     * 获取问卷详细统计
     */
    public Map<String, Object> getSurveyStats(String templateUuid) {
        SurveyTemplate template = templateRepository.findByUuid(templateUuid);
        if (template == null) {
            return Map.of();
        }

        long submissionCount = templateRepository.countSubmissions(template.getId());
        BigDecimal avgScore = answerRepository.avgScoreByTemplate(template.getId());
        BigDecimal maxScore = answerRepository.maxScoreByTemplate(template.getId());
        BigDecimal minScore = answerRepository.minScoreByTemplate(template.getId());

        return Map.of(
                "uuid", templateUuid,
                "title", template.getTitle(),
                "submissionCount", submissionCount,
                "avgScore", avgScore != null ? avgScore : BigDecimal.ZERO,
                "maxScore", maxScore != null ? maxScore : BigDecimal.ZERO,
                "minScore", minScore != null ? minScore : BigDecimal.ZERO
        );
    }
}