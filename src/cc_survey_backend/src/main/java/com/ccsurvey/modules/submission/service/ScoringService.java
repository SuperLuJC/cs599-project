package com.ccsurvey.modules.submission.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 评分服务
 */
@Slf4j
@Service
public class ScoringService {

    /**
     * 计算问卷总分
     *
     * @param schemaJson 问卷Schema
     * @param answerData 用户答案
     * @return 总分
     */
    public BigDecimal calculateScore(Map<String, Object> schemaJson, Map<String, Object> answerData) {
        if (schemaJson == null || answerData == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal totalScore = BigDecimal.ZERO;

        Object fieldsObj = schemaJson.get("fields");
        if (!(fieldsObj instanceof List)) {
            return BigDecimal.ZERO;
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> fields = (List<Map<String, Object>>) fieldsObj;

        // 展开所有字段（包括子字段）
        List<Map<String, Object>> allFields = new ArrayList<>();
        flattenFields(fields, allFields);

        for (Map<String, Object> field : allFields) {
            // 优先使用 name，如果没有则使用 id
            String fieldName = (String) field.get("name");
            if (fieldName == null || fieldName.isEmpty()) {
                fieldName = (String) field.get("id");
            }
            Object userAnswer = answerData.get(fieldName);

            if (userAnswer == null || "".equals(userAnswer.toString().trim())) {
                continue;
            }

            // 计算该字段的得分
            BigDecimal fieldScore = calculateFieldScore(field, userAnswer);
            totalScore = totalScore.add(fieldScore);
        }

        return totalScore;
    }

    /**
     * 展开字段（处理嵌套字段）
     */
    private void flattenFields(List<Map<String, Object>> fields, List<Map<String, Object>> result) {
        for (Map<String, Object> field : fields) {
            result.add(field);

            // 处理multi-input类型的子字段
            if ("multi-input".equals(field.get("type")) && field.containsKey("subFields")) {
                Object subFieldsObj = field.get("subFields");
                if (subFieldsObj instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> subFields = (List<Map<String, Object>>) subFieldsObj;
                    flattenFields(subFields, result);
                }
            }
        }
    }

    /**
     * 计算单个字段的得分
     */
    @SuppressWarnings("unchecked")
    private BigDecimal calculateFieldScore(Map<String, Object> field, Object userAnswer) {
        BigDecimal score = BigDecimal.ZERO;

        // 如果字段有选项（radio, checkbox, select）
        if (field.containsKey("options")) {
            Object optionsObj = field.get("options");
            if (optionsObj instanceof List) {
                List<Map<String, Object>> options = (List<Map<String, Object>>) optionsObj;

                if (userAnswer instanceof List) {
                    // 多选（checkbox）
                    for (Object answer : (List<?>) userAnswer) {
                        score = score.add(getOptionScore(options, String.valueOf(answer)));
                    }
                } else {
                    // 单选（radio, select）
                    score = getOptionScore(options, String.valueOf(userAnswer));
                }
            }
        } else if (field.containsKey("score")) {
            // 字段直接有分数（input等）
            try {
                Object scoreObj = field.get("score");
                if (scoreObj != null && !"".equals(scoreObj.toString().trim())) {
                    score = new BigDecimal(scoreObj.toString());
                }
            } catch (NumberFormatException e) {
                log.warn("无效的分数值: {}", field.get("score"));
            }
        }

        return score;
    }

    /**
     * 获取选项的分数
     */
    private BigDecimal getOptionScore(List<Map<String, Object>> options, String value) {
        for (Map<String, Object> option : options) {
            String optionValue = String.valueOf(option.get("value"));
            if (value.equals(optionValue)) {
                Object scoreObj = option.get("score");
                if (scoreObj != null) {
                    try {
                        return new BigDecimal(scoreObj.toString());
                    } catch (NumberFormatException e) {
                        return BigDecimal.ZERO;
                    }
                }
            }
        }
        return BigDecimal.ZERO;
    }

    /**
     * 获取问卷最大分数
     */
    public BigDecimal getMaxScore(Map<String, Object> schemaJson) {
        if (schemaJson == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal maxScore = BigDecimal.ZERO;

        Object fieldsObj = schemaJson.get("fields");
        if (!(fieldsObj instanceof List)) {
            return BigDecimal.ZERO;
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> fields = (List<Map<String, Object>>) fieldsObj;

        List<Map<String, Object>> allFields = new ArrayList<>();
        flattenFields(fields, allFields);

        for (Map<String, Object> field : allFields) {
            maxScore = maxScore.add(getFieldMaxScore(field));
        }

        return maxScore;
    }

    /**
     * 获取字段最大分数
     */
    @SuppressWarnings("unchecked")
    private BigDecimal getFieldMaxScore(Map<String, Object> field) {
        BigDecimal maxScore = BigDecimal.ZERO;

        if (field.containsKey("options")) {
            Object optionsObj = field.get("options");
            if (optionsObj instanceof List) {
                List<Map<String, Object>> options = (List<Map<String, Object>>) optionsObj;

                // 找出最高分选项
                for (Map<String, Object> option : options) {
                    Object scoreObj = option.get("score");
                    if (scoreObj != null) {
                        try {
                            BigDecimal score = new BigDecimal(scoreObj.toString());
                            if (score.compareTo(maxScore) > 0) {
                                maxScore = score;
                            }
                        } catch (NumberFormatException e) {
                            // ignore
                        }
                    }
                }
            }
        } else if (field.containsKey("score")) {
            try {
                Object scoreObj = field.get("score");
                if (scoreObj != null && !"".equals(scoreObj.toString().trim())) {
                    maxScore = new BigDecimal(scoreObj.toString());
                }
            } catch (NumberFormatException e) {
                // ignore
            }
        }

        return maxScore;
    }

    /**
     * 获取答案详情列表
     */
    public List<Map<String, Object>> getAnswerDetails(Map<String, Object> schemaJson, Map<String, Object> answerData) {
        List<Map<String, Object>> details = new ArrayList<>();

        log.debug("getAnswerDetails: schemaJson={}, answerData={}", schemaJson != null ? "not null" : "null", answerData != null ? answerData : "null");

        if (schemaJson == null || answerData == null) {
            return details;
        }

        Object fieldsObj = schemaJson.get("fields");
        if (!(fieldsObj instanceof List)) {
            log.debug("fieldsObj is not a List: {}", fieldsObj != null ? fieldsObj.getClass() : "null");
            return details;
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> fields = (List<Map<String, Object>>) fieldsObj;
        log.debug("fields count: {}", fields.size());

        List<Map<String, Object>> allFields = new ArrayList<>();
        flattenFields(fields, allFields);

        for (Map<String, Object> field : allFields) {
            // 优先使用 name，如果没有则使用 id
            String fieldName = (String) field.get("name");
            if (fieldName == null || fieldName.isEmpty()) {
                fieldName = (String) field.get("id");
            }
            String label = (String) field.get("label");
            Object userAnswer = answerData.get(fieldName);

            log.debug("field: name={}, label={}, userAnswer={}", fieldName, label, userAnswer);

            if (userAnswer == null) {
                continue;
            }

            BigDecimal score = calculateFieldScore(field, userAnswer);

            details.add(Map.of(
                    "question", label != null ? label : fieldName,
                    "value", userAnswer,
                    "score", score,
                    "isCorrect", score.compareTo(BigDecimal.ZERO) > 0
            ));
        }

        return details;
    }
}