package com.ccsurvey.modules.survey.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ccsurvey.common.response.ApiResponse;
import com.ccsurvey.common.response.PageResponse;
import com.ccsurvey.modules.log.annotation.OperLog;
import com.ccsurvey.modules.survey.entity.SurveyAnswer;
import com.ccsurvey.modules.survey.entity.SurveyTemplate;
import com.ccsurvey.modules.survey.repository.SurveyAnswerRepository;
import com.ccsurvey.modules.survey.repository.SurveyTemplateRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 答卷管理控制器 (管理员)
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/answers")
@RequiredArgsConstructor
public class AnswerAdminController {

    private final SurveyAnswerRepository answerRepository;
    private final SurveyTemplateRepository templateRepository;

    /**
     * 获取答卷列表
     */
    @GetMapping
    @OperLog(title = "答卷管理", businessType = "SELECT")
    public ApiResponse<PageResponse<Map<String, Object>>> getAnswers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String formId,
            HttpServletRequest request) {

        // 构建查询条件
        LambdaQueryWrapper<SurveyAnswer> wrapper = new LambdaQueryWrapper<>();

        if (formId != null && !formId.isEmpty()) {
            wrapper.eq(SurveyAnswer::getTemplateId, formId);
        }

        wrapper.orderByDesc(SurveyAnswer::getCreateTime);

        // 分页查询 (MyBatis-Plus会自动处理逻辑删除)
        IPage<SurveyAnswer> pageResult = answerRepository.selectPage(
                new Page<>(page, size), wrapper);

        // 转换为前端需要的格式
        List<Map<String, Object>> list = pageResult.getRecords().stream()
                .map(this::convertToMap)
                .collect(Collectors.toList());

        PageResponse<Map<String, Object>> response = new PageResponse<>();
        response.setList(list);
        response.setTotal((int) pageResult.getTotal());
        response.setPage(page);
        response.setSize(size);

        return ApiResponse.success(response);
    }

    /**
     * 获取答卷详情
     */
    @GetMapping("/{uuid}")
    public ApiResponse<Map<String, Object>> getAnswerDetail(@PathVariable String uuid) {
        SurveyAnswer answer = answerRepository.findByUuid(uuid);
        if (answer == null) {
            return ApiResponse.error(404, "答卷不存在");
        }

        Map<String, Object> result = convertToMap(answer);
        result.put("answerData", answer.getAnswerData());

        return ApiResponse.success(result);
    }

    /**
     * 删除答卷
     */
    @DeleteMapping("/{uuid}")
    @OperLog(title = "答卷管理", businessType = "DELETE")
    public ApiResponse<Void> deleteAnswer(@PathVariable String uuid) {
        SurveyAnswer answer = answerRepository.findByUuid(uuid);
        if (answer == null) {
            return ApiResponse.error(404, "答卷不存在");
        }

        // 使用 deleteById，MyBatis-Plus 会自动处理逻辑删除
        answerRepository.deleteById(uuid);

        log.info("答卷删除成功: uuid={}", uuid);
        return ApiResponse.success("删除成功");
    }

    /**
     * 下载答卷详情
     */
    @GetMapping("/download/{uuid}")
    @OperLog(title = "答卷管理", businessType = "EXPORT")
    public void downloadAnswer(@PathVariable String uuid, HttpServletResponse response) throws IOException {
        SurveyAnswer answer = answerRepository.findByUuid(uuid);
        if (answer == null) {
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":404,\"message\":\"答卷不存在\"}");
            return;
        }

        // 获取问卷信息
        SurveyTemplate template = templateRepository.selectById(answer.getTemplateId());
        String surveyTitle = template != null ? template.getTitle() : "未知问卷";

        // 构建下载内容
        Map<String, Object> downloadData = new HashMap<>();
        downloadData.put("surveyTitle", surveyTitle);
        downloadData.put("submitTime", answer.getCreateTime() != null ? answer.getCreateTime().toString() : null);
        downloadData.put("totalScore", answer.getTotalScore());
        downloadData.put("answers", answer.getAnswerData());

        // 转换为JSON
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        String jsonContent = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(downloadData);

        // 设置响应头
        response.setContentType("application/json;charset=UTF-8");
        String fileName = URLEncoder.encode(surveyTitle + "_答卷.json", StandardCharsets.UTF_8);
        response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");

        response.getWriter().write(jsonContent);
    }

    /**
     * 转换为Map
     */
    private Map<String, Object> convertToMap(SurveyAnswer answer) {
        Map<String, Object> map = new HashMap<>();
        map.put("uuid", answer.getId());
        map.put("templateUuid", answer.getTemplateId());
        map.put("totalScore", answer.getTotalScore());
        map.put("createTime", answer.getCreateTime());
        map.put("submitIp", answer.getSubmitIp());

        // 获取问卷标题
        SurveyTemplate template = templateRepository.selectById(answer.getTemplateId());
        map.put("surveyTitle", template != null ? template.getTitle() : "未知问卷");

        // 使用存储的提交人姓名
        String submitterName = answer.getSubmitterName();
        if (submitterName != null && !submitterName.isEmpty()) {
            map.put("name", submitterName);
        } else if (answer.getUserId() != null) {
            map.put("name", "用户" + answer.getUserId());
        } else {
            map.put("name", "匿名用户");
        }

        return map;
    }
}