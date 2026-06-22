package com.ccsurvey.modules.agent.tool.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ccsurvey.modules.agent.tool.AgentTool;
import com.ccsurvey.modules.agent.tool.ToolContext;
import com.ccsurvey.modules.agent.tool.ToolResult;
import com.ccsurvey.modules.survey.entity.SurveyAnswer;
import com.ccsurvey.modules.survey.entity.SurveyTemplate;
import com.ccsurvey.modules.survey.repository.SurveyAnswerRepository;
import com.ccsurvey.modules.survey.repository.SurveyTemplateRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * 数据分析工具
 * 查询问卷提交数据并进行统计分析
 */
@Component
@RequiredArgsConstructor
public class DataAnalysisTool extends AgentTool {

    private final SurveyTemplateRepository templateRepository;
    private final SurveyAnswerRepository answerRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String getName() {
        return "analyze_data";
    }

    @Override
    public String getDescription() {
        return """
                分析问卷数据。支持查询提交记录、统计分数分布、字段值分布等分析功能。

                支持的分析类型 (analysis_type):
                - global_stats: 全局统计（问卷总数、提交总数等，不需要survey_uuid）
                - survey_list: 问卷列表（查看所有问卷及其提交数量）
                - list: 查看某个问卷的提交列表
                - stats: 查看某个问卷的统计信息（总数、平均分、分数分布等）
                - field_dist: 查看某个问卷某个字段的值分布
                - cross_analysis: 某个问卷两个字段的交叉分析

                参数说明:
                - analysis_type (必填): 分析类型
                - survey_uuid: 问卷UUID（global_stats和survey_list不需要此参数）

                重要：用户问"问卷总数"、"提交总数"、"统计总览"时，使用global_stats类型。
                """;
    }

    @Override
    public Map<String, Object> getParametersSchema() {
        return createSchema(
                List.of(
                        Property.of("analysis_type", "string", "分析类型: global_stats(全局统计), survey_list(问卷列表), list(提交列表), stats(统计分析), field_dist(字段分布), cross_analysis(交叉分析)"),
                        Property.of("survey_uuid", "string", "问卷UUID（global_stats和survey_list不需要）"),
                        Property.of("field_name", "string", "字段名称 (字段分布分析时必填)"),
                        Property.of("cross_field1", "string", "交叉分析字段1"),
                        Property.of("cross_field2", "string", "交叉分析字段2"),
                        Property.of("filters", "object", "过滤条件，如 {\"field1\": \"value1\"}"),
                        Property.of("limit", "integer", "返回记录数量限制 (默认20)")
                ),
                List.of("analysis_type")
        );
    }

    @Override
    public ToolResult execute(Map<String, Object> arguments, ToolContext context) {
        try {
            String analysisType = (String) arguments.get("analysis_type");
            if (analysisType == null || analysisType.trim().isEmpty()) {
                analysisType = "global_stats";
            }

            // 全局统计和问卷列表不需要survey_uuid
            if ("global_stats".equals(analysisType)) {
                return executeGlobalStats();
            } else if ("survey_list".equals(analysisType)) {
                return executeSurveyList();
            }

            // 其他分析类型需要survey_uuid
            String surveyUuid = (String) arguments.get("survey_uuid");
            if (surveyUuid == null || surveyUuid.trim().isEmpty()) {
                return ToolResult.error("请指定要分析的问卷UUID，或使用 survey_list 查看所有问卷列表");
            }

            int limit = arguments.get("limit") != null ? ((Number) arguments.get("limit")).intValue() : 20;

            // 验证问卷存在
            SurveyTemplate template = templateRepository.selectById(surveyUuid);
            if (template == null) {
                return ToolResult.error("问卷不存在: " + surveyUuid);
            }

            return switch (analysisType) {
                case "list" -> executeList(surveyUuid, limit);
                case "stats" -> executeStats(surveyUuid);
                case "field_dist" -> executeFieldDistribution(surveyUuid, (String) arguments.get("field_name"));
                case "cross_analysis" -> executeCrossAnalysis(
                        surveyUuid,
                        (String) arguments.get("cross_field1"),
                        (String) arguments.get("cross_field2")
                );
                default -> ToolResult.error("未知的分析类型: " + analysisType);
            };

        } catch (Exception e) {
            return ToolResult.error("数据分析失败: " + e.getMessage());
        }
    }

    /**
     * 全局统计 - 统计所有问卷和提交数据
     */
    private ToolResult executeGlobalStats() {
        // 查询所有问卷
        List<SurveyTemplate> templates = templateRepository.selectList(new LambdaQueryWrapper<>());

        int totalSurveys = templates.size();
        int draftSurveys = 0;
        int activeSurveys = 0;

        int totalSubmissions = 0;
        String latestSubmitTime = null;
        String mostActiveSurvey = null;
        int mostActiveCount = 0;

        for (SurveyTemplate template : templates) {
            // 统计问卷状态
            if ("草稿".equals(template.getStatus())) {
                draftSurveys++;
            } else {
                activeSurveys++;
            }

            // 统计该问卷的提交数
            LambdaQueryWrapper<SurveyAnswer> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SurveyAnswer::getTemplateId, template.getId());
            long submitCount = answerRepository.selectCount(wrapper);
            totalSubmissions += submitCount;

            // 找出最活跃的问卷
            if (submitCount > mostActiveCount) {
                mostActiveCount = (int) submitCount;
                mostActiveSurvey = template.getTitle();
            }

            // 查找最近提交时间
            LambdaQueryWrapper<SurveyAnswer> timeWrapper = new LambdaQueryWrapper<>();
            timeWrapper.eq(SurveyAnswer::getTemplateId, template.getId())
                    .orderByDesc(SurveyAnswer::getCreateTime)
                    .last("LIMIT 1");
            SurveyAnswer latestAnswer = answerRepository.selectOne(timeWrapper);
            if (latestAnswer != null && latestAnswer.getCreateTime() != null) {
                String answerTime = latestAnswer.getCreateTime().toString();
                if (latestSubmitTime == null || answerTime.compareTo(latestSubmitTime) > 0) {
                    latestSubmitTime = answerTime;
                }
            }
        }

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total_surveys", totalSurveys);
        stats.put("active_surveys", activeSurveys);
        stats.put("draft_surveys", draftSurveys);
        stats.put("total_submissions", totalSubmissions);
        stats.put("most_active_survey", mostActiveSurvey);
        stats.put("most_active_count", mostActiveCount);
        if (latestSubmitTime != null) {
            stats.put("latest_submit_time", latestSubmitTime);
        }

        return ToolResult.success(stats);
    }

    /**
     * 问卷列表 - 返回所有问卷及其提交数量
     */
    private ToolResult executeSurveyList() {
        List<SurveyTemplate> templates = templateRepository.selectList(new LambdaQueryWrapper<>());

        List<Map<String, Object>> surveyList = new ArrayList<>();
        for (SurveyTemplate template : templates) {
            // 统计该问卷的提交数
            LambdaQueryWrapper<SurveyAnswer> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SurveyAnswer::getTemplateId, template.getId());
            long submitCount = answerRepository.selectCount(wrapper);

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("survey_uuid", template.getId());
            item.put("title", template.getTitle());
            item.put("status", template.getStatus());
            item.put("submit_count", submitCount);
            item.put("create_time", template.getCreateTime() != null ? template.getCreateTime().toString() : null);

            surveyList.add(item);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total_count", templates.size());
        result.put("surveys", surveyList);

        return ToolResult.success(result);
    }

    /**
     * 列表查询
     */
    private ToolResult executeList(String surveyUuid, int limit) {
        LambdaQueryWrapper<SurveyAnswer> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SurveyAnswer::getTemplateId, surveyUuid)
                .orderByDesc(SurveyAnswer::getCreateTime)
                .last("LIMIT " + limit);

        List<SurveyAnswer> answers = answerRepository.selectList(wrapper);

        List<Map<String, Object>> result = new ArrayList<>();
        for (SurveyAnswer answer : answers) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("answer_uuid", answer.getId());
            item.put("submitter", answer.getSubmitterName() != null ? answer.getSubmitterName() : "匿名");
            item.put("total_score", answer.getTotalScore());
            item.put("submit_time", answer.getCreateTime().toString());
            item.put("answer_data", answer.getAnswerData());
            result.add(item);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("survey_title", "问卷提交列表");
        data.put("total_count", answers.size());
        data.put("records", result);

        return ToolResult.success(data);
    }

    /**
     * 统计分析
     */
    private ToolResult executeStats(String surveyUuid) {
        LambdaQueryWrapper<SurveyAnswer> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SurveyAnswer::getTemplateId, surveyUuid);

        List<SurveyAnswer> answers = answerRepository.selectList(wrapper);

        if (answers.isEmpty()) {
            return ToolResult.success(Map.of(
                    "message", "暂无提交数据",
                    "total_count", 0
            ));
        }

        // 计算统计数据
        int totalCount = answers.size();
        BigDecimal totalScore = BigDecimal.ZERO;
        BigDecimal maxScore = null;
        BigDecimal minScore = null;
        int scoredCount = 0;

        for (SurveyAnswer answer : answers) {
            if (answer.getTotalScore() != null) {
                totalScore = totalScore.add(answer.getTotalScore());
                if (maxScore == null || answer.getTotalScore().compareTo(maxScore) > 0) {
                    maxScore = answer.getTotalScore();
                }
                if (minScore == null || answer.getTotalScore().compareTo(minScore) < 0) {
                    minScore = answer.getTotalScore();
                }
                scoredCount++;
            }
        }

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total_submissions", totalCount);
        stats.put("scored_submissions", scoredCount);

        if (scoredCount > 0) {
            stats.put("avg_score", totalScore.divide(BigDecimal.valueOf(scoredCount), 2, RoundingMode.HALF_UP));
            stats.put("max_score", maxScore);
            stats.put("min_score", minScore);
        }

        // 分数分布
        Map<String, Integer> scoreDistribution = new LinkedHashMap<>();
        scoreDistribution.put("0-60", 0);
        scoreDistribution.put("60-70", 0);
        scoreDistribution.put("70-80", 0);
        scoreDistribution.put("80-90", 0);
        scoreDistribution.put("90-100", 0);

        for (SurveyAnswer answer : answers) {
            if (answer.getTotalScore() != null) {
                int score = answer.getTotalScore().intValue();
                if (score < 60) scoreDistribution.merge("0-60", 1, Integer::sum);
                else if (score < 70) scoreDistribution.merge("60-70", 1, Integer::sum);
                else if (score < 80) scoreDistribution.merge("70-80", 1, Integer::sum);
                else if (score < 90) scoreDistribution.merge("80-90", 1, Integer::sum);
                else scoreDistribution.merge("90-100", 1, Integer::sum);
            }
        }
        stats.put("score_distribution", scoreDistribution);

        return ToolResult.success(stats);
    }

    /**
     * 字段分布分析
     */
    @SuppressWarnings("unchecked")
    private ToolResult executeFieldDistribution(String surveyUuid, String fieldName) {
        if (fieldName == null) {
            return ToolResult.error("字段分布分析需要指定 field_name");
        }

        LambdaQueryWrapper<SurveyAnswer> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SurveyAnswer::getTemplateId, surveyUuid);

        List<SurveyAnswer> answers = answerRepository.selectList(wrapper);

        Map<Object, Integer> distribution = new LinkedHashMap<>();

        for (SurveyAnswer answer : answers) {
            if (answer.getAnswerData() != null) {
                Object value = answer.getAnswerData().get(fieldName);
                if (value != null) {
                    distribution.merge(value, 1, Integer::sum);
                }
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("field_name", fieldName);
        result.put("total_answers", answers.size());
        result.put("distribution", distribution);

        return ToolResult.success(result);
    }

    /**
     * 交叉分析
     */
    @SuppressWarnings("unchecked")
    private ToolResult executeCrossAnalysis(String surveyUuid, String field1, String field2) {
        if (field1 == null || field2 == null) {
            return ToolResult.error("交叉分析需要指定两个字段");
        }

        LambdaQueryWrapper<SurveyAnswer> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SurveyAnswer::getTemplateId, surveyUuid);

        List<SurveyAnswer> answers = answerRepository.selectList(wrapper);

        // 交叉表: field1 -> field2 -> count
        Map<Object, Map<Object, Integer>> crossTable = new LinkedHashMap<>();

        for (SurveyAnswer answer : answers) {
            if (answer.getAnswerData() != null) {
                Object val1 = answer.getAnswerData().get(field1);
                Object val2 = answer.getAnswerData().get(field2);

                if (val1 != null && val2 != null) {
                    crossTable
                            .computeIfAbsent(val1, k -> new LinkedHashMap<>())
                            .merge(val2, 1, Integer::sum);
                }
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("field1", field1);
        result.put("field2", field2);
        result.put("total_answers", answers.size());
        result.put("cross_table", crossTable);

        return ToolResult.success(result);
    }
}
