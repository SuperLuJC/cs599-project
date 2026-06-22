package com.ccsurvey.modules.survey.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Map;

/**
 * 创建问卷请求DTO
 */
@Data
public class SurveyCreateRequest {

    @NotBlank(message = "问卷标题不能为空")
    @Size(max = 200, message = "标题长度不能超过200个字符")
    private String title;

    @Size(max = 1000, message = "描述长度不能超过1000个字符")
    private String description;

    private String formId;

    /**
     * 表单JSON Schema
     */
    private Map<String, Object> schemaJson;

    /**
     * 开始时间
     */
    private String startTime;

    /**
     * 结束时间
     */
    private String endTime;

    /**
     * 最大提交数
     */
    private Integer maxSubmissions;

    /**
     * 是否允许修改
     */
    private Boolean allowEdit;

    /**
     * 是否允许匿名
     */
    private Boolean allowAnonymous;
}