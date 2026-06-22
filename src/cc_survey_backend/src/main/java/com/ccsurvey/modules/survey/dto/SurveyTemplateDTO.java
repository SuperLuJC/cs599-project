package com.ccsurvey.modules.survey.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 问卷模板DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SurveyTemplateDTO {

    private String uuid;
    private String title;
    private String description;
    private String formId;
    private Map<String, Object> schemaJson;
    private Integer status;
    private Integer version;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer maxSubmissions;
    private Boolean allowEdit;
    private Boolean allowAnonymous;
    private String createdByUuid;
    private String createdByName;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private LocalDateTime publishTime;

    // 统计信息
    private Long submissionCount;
    private BigDecimal avgScore;
    private BigDecimal maxScore;
    private BigDecimal minScore;
}