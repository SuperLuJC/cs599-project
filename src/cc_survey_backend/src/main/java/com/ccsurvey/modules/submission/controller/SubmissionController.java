package com.ccsurvey.modules.submission.controller;

import com.ccsurvey.common.response.ApiResponse;
import com.ccsurvey.modules.log.annotation.OperLog;
import com.ccsurvey.modules.submission.dto.SubmitRequest;
import com.ccsurvey.modules.submission.service.SubmissionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 提交控制器
 */
@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SubmissionController {

    private final SubmissionService submissionService;

    /**
     * 提交问卷
     */
    @PostMapping("/submit")
    @OperLog(title = "问卷提交", businessType = "INSERT")
    public ApiResponse<Map<String, Object>> submit(
            @Valid @RequestBody SubmitRequest request,
            HttpServletRequest httpRequest) {

        String userId = (String) httpRequest.getAttribute("userId");
        String name = (String) httpRequest.getAttribute("name"); // 使用真实姓名

        // 支持匿名提交 - userId和name保持为null
        Map<String, Object> result = submissionService.submit(request, userId, name, httpRequest);
        return ApiResponse.success("提交成功", result);
    }

    /**
     * 获取用户在某问卷的最新提交结果
     */
    @GetMapping("/surveys/{templateUuid}/my-submission")
    public ApiResponse<Map<String, Object>> getMySubmission(
            @PathVariable String templateUuid,
            HttpServletRequest httpRequest) {

        String userId = (String) httpRequest.getAttribute("userId");
        if (userId == null) {
            return ApiResponse.error(com.ccsurvey.common.response.ErrorCode.UNAUTHORIZED);
        }

        Map<String, Object> result = submissionService.getUserSubmissionResult(templateUuid, userId);
        return ApiResponse.success(result);
    }

    /**
     * 删除旧答案（允许重新填写）
     */
    @DeleteMapping("/surveys/{templateUuid}/answer")
    public ApiResponse<Void> deleteAnswer(
            @PathVariable String templateUuid,
            HttpServletRequest httpRequest) {

        String userId = (String) httpRequest.getAttribute("userId");
        if (userId == null) {
            return ApiResponse.error(com.ccsurvey.common.response.ErrorCode.UNAUTHORIZED);
        }

        submissionService.deletePreviousAnswer(templateUuid, userId);
        return ApiResponse.success("已清除旧答案，可以重新填写");
    }
}