package com.ccsurvey.modules.survey.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ccsurvey.common.exception.BusinessException;
import com.ccsurvey.common.response.ErrorCode;
import com.ccsurvey.common.response.PageResponse;
import com.ccsurvey.common.util.RedisUtils;
import com.ccsurvey.modules.survey.dto.SurveyCreateRequest;
import com.ccsurvey.modules.survey.dto.SurveyTemplateDTO;
import com.ccsurvey.modules.survey.entity.SurveyAnswer;
import com.ccsurvey.modules.survey.entity.SurveyTemplate;
import com.ccsurvey.modules.survey.repository.SurveyAnswerRepository;
import com.ccsurvey.modules.survey.repository.SurveyTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 问卷服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SurveyService {

    private final SurveyTemplateRepository templateRepository;
    private final SurveyAnswerRepository answerRepository;
    private final RedisUtils redisUtils;

    private static final String CACHE_PREFIX = "survey:template:";
    private static final long CACHE_TTL_MINUTES = 30;

    /**
     * 创建问卷
     */
    @Transactional
    public SurveyTemplateDTO createSurvey(SurveyCreateRequest request, String userId, String username) {
        SurveyTemplate template = new SurveyTemplate();
        template.setId(UUID.randomUUID().toString().replace("-", ""));
        template.setTitle(request.getTitle());
        template.setDescription(request.getDescription());
        template.setFormId(request.getFormId());
        template.setSchemaJson(request.getSchemaJson());
        template.setStatus(0); // 草稿状态
        template.setVersion(1);
        template.setMaxSubmissions(request.getMaxSubmissions() != null ? request.getMaxSubmissions() : 0);
        template.setAllowEdit(request.getAllowEdit() != null && request.getAllowEdit() ? 1 : 0);
        template.setAllowAnonymous(request.getAllowAnonymous() != null && request.getAllowAnonymous() ? 1 : 0);
        template.setCreatedBy(userId);
        template.setCreatedByName(username);

        // 解析时间
        if (request.getStartTime() != null) {
            template.setStartTime(parseDateTime(request.getStartTime()));
        }
        if (request.getEndTime() != null) {
            template.setEndTime(parseDateTime(request.getEndTime()));
        }

        templateRepository.insert(template);

        log.info("问卷创建成功: id={}, title={}", template.getId(), template.getTitle());

        return convertToDTO(template);
    }

    /**
     * 更新问卷
     */
    @Transactional
    public SurveyTemplateDTO updateSurvey(String uuid, SurveyCreateRequest request) {
        SurveyTemplate template = templateRepository.findByUuid(uuid);
        if (template == null) {
            throw new BusinessException(ErrorCode.SURVEY_NOT_FOUND);
        }

        // 已发布的问卷不能修改
        if (template.isPublished()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "已发布的问卷不能修改");
        }

        template.setTitle(request.getTitle());
        template.setDescription(request.getDescription());
        template.setSchemaJson(request.getSchemaJson());
        template.setMaxSubmissions(request.getMaxSubmissions() != null ? request.getMaxSubmissions() : 0);
        template.setAllowEdit(request.getAllowEdit() != null && request.getAllowEdit() ? 1 : 0);
        template.setAllowAnonymous(request.getAllowAnonymous() != null && request.getAllowAnonymous() ? 1 : 0);

        if (request.getStartTime() != null) {
            template.setStartTime(parseDateTime(request.getStartTime()));
        }
        if (request.getEndTime() != null) {
            template.setEndTime(parseDateTime(request.getEndTime()));
        }

        templateRepository.updateById(template);

        // 清除缓存
        redisUtils.deleteCache(CACHE_PREFIX + uuid);

        log.info("问卷更新成功: uuid={}", uuid);

        return convertToDTO(template);
    }

    /**
     * 发布问卷
     */
    @Transactional
    public void publishSurvey(String uuid) {
        SurveyTemplate template = templateRepository.findByUuid(uuid);
        if (template == null) {
            throw new BusinessException(ErrorCode.SURVEY_NOT_FOUND);
        }

        if (template.isPublished()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "问卷已发布");
        }

        template.setStatus(1);
        template.setPublishTime(LocalDateTime.now());
        templateRepository.updateById(template);

        // 缓存问卷
        cacheTemplate(template);

        // 初始化提交计数器（如果有提交限制）
        Integer maxSubmissions = template.getMaxSubmissions();
        if (maxSubmissions != null && maxSubmissions > 0) {
            long currentCount = templateRepository.countSubmissions(template.getId());
            redisUtils.initSubmissionCount(uuid, currentCount, 86400 * 7); // 7天过期
            log.info("初始化提交计数器: uuid={}, maxSubmissions={}, currentCount={}",
                    uuid, maxSubmissions, currentCount);
        }

        log.info("问卷发布成功: uuid={}", uuid);
    }

    /**
     * 归档问卷
     */
    @Transactional
    public void archiveSurvey(String uuid) {
        SurveyTemplate template = templateRepository.findByUuid(uuid);
        if (template == null) {
            throw new BusinessException(ErrorCode.SURVEY_NOT_FOUND);
        }

        template.setStatus(2);
        templateRepository.updateById(template);

        // 清除缓存
        redisUtils.deleteCache(CACHE_PREFIX + uuid);

        log.info("问卷归档成功: uuid={}", uuid);
    }

    /**
     * 删除问卷
     */
    @Transactional
    public void deleteSurvey(String uuid) {
        SurveyTemplate template = templateRepository.findByUuid(uuid);
        if (template == null) {
            throw new BusinessException(ErrorCode.SURVEY_NOT_FOUND);
        }

        // 逻辑删除问卷
        templateRepository.deleteById(uuid);

        // 清除缓存
        redisUtils.deleteCache(CACHE_PREFIX + uuid);

        log.info("问卷删除成功: uuid={}", uuid);
    }

    /**
     * 获取问卷详情
     */
    public SurveyTemplateDTO getSurvey(String uuid) {
        // 先从缓存获取
        SurveyTemplate template = getCachedTemplate(uuid);
        if (template == null) {
            template = templateRepository.findByUuid(uuid);
            if (template == null) {
                throw new BusinessException(ErrorCode.SURVEY_NOT_FOUND);
            }
            // 缓存问卷
            cacheTemplate(template);
        }

        return convertToDTO(template);
    }

    /**
     * 获取问卷列表 (分页)
     */
    public PageResponse<SurveyTemplateDTO> getSurveyList(int page, int size, Integer status) {
        LambdaQueryWrapper<SurveyTemplate> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(SurveyTemplate::getStatus, status);
        }
        wrapper.orderByDesc(SurveyTemplate::getCreateTime);

        Page<SurveyTemplate> pageResult = templateRepository.selectPage(new Page<>(page, size), wrapper);

        List<SurveyTemplateDTO> dtoList = pageResult.getRecords().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        return PageResponse.of(dtoList, pageResult.getTotal(), page, size);
    }

    /**
     * 获取用户可填写的问卷列表
     */
    public PageResponse<SurveyTemplateDTO> getAvailableSurveys(int page, int size) {
        LambdaQueryWrapper<SurveyTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SurveyTemplate::getStatus, 1); // 已发布
        wrapper.orderByDesc(SurveyTemplate::getCreateTime);

        Page<SurveyTemplate> pageResult = templateRepository.selectPage(new Page<>(page, size), wrapper);

        List<SurveyTemplateDTO> dtoList = pageResult.getRecords().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        return PageResponse.of(dtoList, pageResult.getTotal(), page, size);
    }

    /**
     * 检查用户是否已提交问卷
     */
    public boolean hasSubmitted(String templateUuid, String userId) {
        SurveyTemplate template = templateRepository.findByUuid(templateUuid);
        if (template == null) {
            return false;
        }
        return answerRepository.countByTemplateAndUser(template.getId(), userId) > 0;
    }

    /**
     * 获取用户在某问卷的最新答案
     */
    public Map<String, Object> getUserAnswer(String templateUuid, String userId) {
        SurveyTemplate template = templateRepository.findByUuid(templateUuid);
        if (template == null) {
            throw new BusinessException(ErrorCode.SURVEY_NOT_FOUND);
        }

        log.debug("获取用户答案: templateId={}, userId={}", template.getId(), userId);

        List<SurveyAnswer> answers = answerRepository.selectList(
                new LambdaQueryWrapper<SurveyAnswer>()
                        .eq(SurveyAnswer::getTemplateId, template.getId())
                        .eq(SurveyAnswer::getUserId, userId)
                        .orderByDesc(SurveyAnswer::getCreateTime)
                        .last("LIMIT 1")
        );

        if (answers == null || answers.isEmpty()) {
            log.debug("未找到用户答案");
            return null;
        }

        SurveyAnswer answer = answers.get(0);
        log.debug("找到用户答案: answerId={}", answer.getId());

        Map<String, Object> result = new HashMap<>();
        result.put("answerUuid", answer.getId());
        result.put("answerData", answer.getAnswerData() != null ? answer.getAnswerData() : Map.of());
        result.put("submitTime", answer.getCreateTime() != null ?
            answer.getCreateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "");
        return result;
    }

    // ==================== 私有方法 ====================

    /**
     * 缓存问卷模板
     */
    private void cacheTemplate(SurveyTemplate template) {
        // 使用JSON序列化存储到Redis
        // 这里简化处理，实际应该序列化整个对象
        redisUtils.setCache(CACHE_PREFIX + template.getId(), template.getId(), CACHE_TTL_MINUTES);
    }

    /**
     * 从缓存获取问卷模板
     */
    private SurveyTemplate getCachedTemplate(String uuid) {
        // 这里简化处理，实际应该从Redis反序列化
        return null;
    }

    /**
     * 转换为DTO
     */
    private SurveyTemplateDTO convertToDTO(SurveyTemplate template) {
        SurveyTemplateDTO dto = SurveyTemplateDTO.builder()
                .uuid(template.getId())
                .title(template.getTitle())
                .description(template.getDescription())
                .formId(template.getFormId())
                .schemaJson(template.getSchemaJson())
                .status(template.getStatus())
                .version(template.getVersion())
                .startTime(template.getStartTime())
                .endTime(template.getEndTime())
                .maxSubmissions(template.getMaxSubmissions())
                .allowEdit(template.getAllowEdit() == 1)
                .allowAnonymous(template.getAllowAnonymous() == 1)
                .createdByUuid(template.getCreatedBy())
                .createdByName(template.getCreatedByName())
                .createTime(template.getCreateTime())
                .updateTime(template.getUpdateTime())
                .publishTime(template.getPublishTime())
                .build();

        // 添加统计信息
        long submissionCount = templateRepository.countSubmissions(template.getId());
        dto.setSubmissionCount(submissionCount);

        if (submissionCount > 0) {
            dto.setAvgScore(answerRepository.avgScoreByTemplate(template.getId()));
            dto.setMaxScore(answerRepository.maxScoreByTemplate(template.getId()));
            dto.setMinScore(answerRepository.minScoreByTemplate(template.getId()));
        }

        return dto;
    }

    /**
     * 解析日期时间
     */
    private LocalDateTime parseDateTime(String dateTime) {
        if (dateTime == null || dateTime.trim().isEmpty()) {
            return null;
        }
        log.info("解析时间字符串: {}", dateTime);
        try {
            // 尝试 ISO 格式 (带T分隔符)
            return LocalDateTime.parse(dateTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (Exception e1) {
            try {
                // 尝试标准格式
                return LocalDateTime.parse(dateTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            } catch (Exception e2) {
                try {
                    // 尝试不带秒的格式
                    return LocalDateTime.parse(dateTime, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));
                } catch (Exception e3) {
                    log.error("无法解析时间: {}", dateTime);
                    return null;
                }
            }
        }
    }
}