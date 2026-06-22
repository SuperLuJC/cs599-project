package com.ccsurvey.modules.survey.controller;

import com.ccsurvey.common.exception.BusinessException;
import com.ccsurvey.common.response.ApiResponse;
import com.ccsurvey.common.response.ErrorCode;
import com.ccsurvey.common.response.PageResponse;
import com.ccsurvey.modules.log.annotation.OperLog;
import com.ccsurvey.modules.survey.dto.SurveyCreateRequest;
import com.ccsurvey.modules.survey.dto.SurveyTemplateDTO;
import com.ccsurvey.modules.survey.service.SurveyService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 问卷管理控制器 (管理员)
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/surveys")
@RequiredArgsConstructor
public class SurveyAdminController {

    private final SurveyService surveyService;

    /**
     * 获取问卷列表 (管理员)
     */
    @GetMapping
    @OperLog(title = "问卷管理", businessType = "SELECT")
    public ApiResponse<PageResponse<SurveyTemplateDTO>> getSurveyList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer status) {

        PageResponse<SurveyTemplateDTO> result = surveyService.getSurveyList(page, size, status);
        return ApiResponse.success(result);
    }

    /**
     * 创建问卷
     */
    @PostMapping
    @OperLog(title = "问卷管理", businessType = "INSERT")
    public ApiResponse<SurveyTemplateDTO> createSurvey(
            @Valid @RequestBody SurveyCreateRequest request,
            HttpServletRequest httpRequest) {

        String userId = (String) httpRequest.getAttribute("userId");
        String username = (String) httpRequest.getAttribute("username");
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        SurveyTemplateDTO survey = surveyService.createSurvey(request, userId, username);
        return ApiResponse.success("问卷创建成功", survey);
    }

    /**
     * 更新问卷
     */
    @PutMapping("/{uuid}")
    @OperLog(title = "问卷管理", businessType = "UPDATE")
    public ApiResponse<SurveyTemplateDTO> updateSurvey(
            @PathVariable String uuid,
            @Valid @RequestBody SurveyCreateRequest request) {

        SurveyTemplateDTO survey = surveyService.updateSurvey(uuid, request);
        return ApiResponse.success("问卷更新成功", survey);
    }

    /**
     * 发布问卷
     */
    @PostMapping("/{uuid}/publish")
    @OperLog(title = "问卷管理", businessType = "UPDATE")
    public ApiResponse<Void> publishSurvey(@PathVariable String uuid) {
        surveyService.publishSurvey(uuid);
        return ApiResponse.success("问卷发布成功");
    }

    /**
     * 归档问卷
     */
    @PostMapping("/{uuid}/archive")
    @OperLog(title = "问卷管理", businessType = "UPDATE")
    public ApiResponse<Void> archiveSurvey(@PathVariable String uuid) {
        surveyService.archiveSurvey(uuid);
        return ApiResponse.success("问卷归档成功");
    }

    /**
     * 删除问卷
     */
    @DeleteMapping("/{uuid}")
    @OperLog(title = "问卷管理", businessType = "DELETE")
    public ApiResponse<Void> deleteSurvey(@PathVariable String uuid) {
        surveyService.deleteSurvey(uuid);
        return ApiResponse.success("问卷删除成功");
    }

    /**
     * 获取问卷详情 (管理员)
     */
    @GetMapping("/{uuid}")
    public ApiResponse<SurveyTemplateDTO> getSurvey(@PathVariable String uuid) {
        SurveyTemplateDTO survey = surveyService.getSurvey(uuid);
        return ApiResponse.success(survey);
    }

    /**
     * 复制问卷
     */
    @PostMapping("/{uuid}/copy")
    public ApiResponse<SurveyTemplateDTO> copySurvey(
            @PathVariable String uuid,
            HttpServletRequest httpRequest) {

        String userId = (String) httpRequest.getAttribute("userId");
        String username = (String) httpRequest.getAttribute("username");
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        // 获取原问卷
        SurveyTemplateDTO original = surveyService.getSurvey(uuid);

        // 创建新问卷
        SurveyCreateRequest request = new SurveyCreateRequest();
        request.setTitle(original.getTitle() + " (副本)");
        request.setDescription(original.getDescription());
        request.setSchemaJson(original.getSchemaJson());
        request.setMaxSubmissions(original.getMaxSubmissions());
        request.setAllowEdit(original.getAllowEdit());
        request.setAllowAnonymous(original.getAllowAnonymous());

        SurveyTemplateDTO newSurvey = surveyService.createSurvey(request, userId, username);
        return ApiResponse.success("问卷复制成功", newSurvey);
    }
}