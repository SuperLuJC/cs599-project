package com.ccsurvey.modules.submission.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

/**
 * 提交请求DTO
 */
@Data
public class SubmitRequest {

    @NotBlank(message = "问卷ID不能为空")
    private String templateUuid;

    @NotNull(message = "答案数据不能为空")
    private Map<String, Object> data;

    /**
     * 填写耗时(秒)
     */
    private Integer durationSeconds;
}