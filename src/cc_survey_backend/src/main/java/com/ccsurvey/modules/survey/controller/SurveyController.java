package com.ccsurvey.modules.survey.controller;

import com.ccsurvey.common.response.ApiResponse;
import com.ccsurvey.common.response.PageResponse;
import com.ccsurvey.common.util.RedisUtils;
import com.ccsurvey.modules.survey.dto.SurveyTemplateDTO;
import com.ccsurvey.modules.survey.service.SurveyService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 问卷控制器 (公开接口)
 */
@Slf4j
@RestController
@RequestMapping("/api/surveys")
@RequiredArgsConstructor
public class SurveyController {

    private final SurveyService surveyService;
    private final RedisUtils redisUtils;

    /**
     * 获取可填写的问卷列表
     */
    @GetMapping("/list")
    public ApiResponse<PageResponse<SurveyTemplateDTO>> getAvailableSurveys(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {

        PageResponse<SurveyTemplateDTO> result = surveyService.getAvailableSurveys(page, size);
        return ApiResponse.success(result);
    }

    /**
     * 获取问卷详情
     */
    @GetMapping("/{uuid}")
    public ApiResponse<SurveyTemplateDTO> getSurvey(@PathVariable String uuid) {
        SurveyTemplateDTO survey = surveyService.getSurvey(uuid);
        return ApiResponse.success(survey);
    }

    /**
     * 检查是否已提交
     */
    @GetMapping("/{uuid}/submitted")
    public ApiResponse<Map<String, Boolean>> checkSubmitted(
            @PathVariable String uuid,
            HttpServletRequest request) {

        String userId = (String) request.getAttribute("userId");
        boolean submitted = surveyService.hasSubmitted(uuid, userId);

        Map<String, Boolean> result = new HashMap<>();
        result.put("submitted", submitted);
        return ApiResponse.success(result);
    }

    /**
     * 获取用户已提交的答案 (用于回显)
     */
    @GetMapping("/{uuid}/my-answer")
    public ApiResponse<Map<String, Object>> getUserAnswer(
            @PathVariable String uuid,
            HttpServletRequest request) {

        String userId = (String) request.getAttribute("userId");
        log.debug("获取用户答案: uuid={}, userId={}", uuid, userId);

        if (userId == null) {
            log.debug("用户未登录，返回null");
            return ApiResponse.success(null);
        }

        try {
            Map<String, Object> answer = surveyService.getUserAnswer(uuid, userId);
            log.debug("获取用户答案结果: {}", answer != null ? "找到答案" : "未找到答案");
            return ApiResponse.success(answer);
        } catch (Exception e) {
            log.error("获取用户答案失败: uuid={}, userId={}, error={}", uuid, userId, e.getMessage(), e);
            return ApiResponse.success(null);
        }
    }

    /**
     * 获取问卷剩余可提交份数
     */
    @GetMapping("/{uuid}/quota")
    public ApiResponse<Map<String, Object>> getSubmissionQuota(@PathVariable String uuid) {
        SurveyTemplateDTO survey = surveyService.getSurvey(uuid);

        Integer maxSubmissions = survey.getMaxSubmissions();
        if (maxSubmissions == null || maxSubmissions <= 0) {
            // 无限制
            return ApiResponse.success(Map.of(
                    "limited", false,
                    "maxSubmissions", 0,
                    "submittedCount", survey.getSubmissionCount(),
                    "remaining", -1  // -1表示无限制
            ));
        }

        // 优先从Redis获取实时计数
        Long submittedCount = redisUtils.getSubmissionCount(uuid);
        if (submittedCount == null) {
            // Redis中没有，使用数据库计数
            submittedCount = survey.getSubmissionCount();
        }

        int remaining = Math.max(0, maxSubmissions - submittedCount.intValue());

        log.debug("获取配额信息: uuid={}, max={}, submitted={}, remaining={}",
                uuid, maxSubmissions, submittedCount, remaining);

        return ApiResponse.success(Map.of(
                "limited", true,
                "maxSubmissions", maxSubmissions,
                "submittedCount", submittedCount,
                "remaining", remaining
        ));
    }
}