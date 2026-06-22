package com.ccsurvey.modules.submission.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ccsurvey.common.exception.BusinessException;
import com.ccsurvey.common.response.ErrorCode;
import com.ccsurvey.common.util.IpUtils;
import com.ccsurvey.common.util.RedisUtils;
import com.ccsurvey.modules.submission.dto.SubmitRequest;
import com.ccsurvey.modules.submission.mq.SubmissionEvent;
import com.ccsurvey.modules.submission.mq.SubmissionProducer;
import com.ccsurvey.modules.survey.entity.SurveyAnswer;
import com.ccsurvey.modules.survey.entity.SurveyTemplate;
import com.ccsurvey.modules.survey.repository.SurveyAnswerRepository;
import com.ccsurvey.modules.survey.repository.SurveyTemplateRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 提交服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SubmissionService {

    private final SurveyTemplateRepository templateRepository;
    private final SurveyAnswerRepository answerRepository;
    private final RedisUtils redisUtils;
    private final SubmissionProducer submissionProducer;
    private final ScoringService scoringService;

    /**
     * 提交问卷
     * 使用双重保护策略防止超发：Redis原子计数器 + 问卷级别分布式锁
     */
    @Transactional
    public Map<String, Object> submit(SubmitRequest request, String userId, String username, HttpServletRequest httpRequest) {
        String templateUuid = request.getTemplateUuid();

        // 1. 获取问卷模板
        SurveyTemplate template = templateRepository.findByUuid(templateUuid);
        if (template == null) {
            throw new BusinessException(ErrorCode.SURVEY_NOT_FOUND);
        }

        // 2. 检查问卷状态
        if (!template.isPublished()) {
            throw new BusinessException(ErrorCode.SURVEY_NOT_PUBLISHED);
        }
        if (template.isExpired()) {
            throw new BusinessException(ErrorCode.SURVEY_EXPIRED);
        }
        if (template.isNotStarted()) {
            throw new BusinessException(ErrorCode.SURVEY_NOT_STARTED);
        }

        // 3. 提交份数限制检查
        Integer maxSubmissions = template.getMaxSubmissions();
        boolean hasLimit = maxSubmissions != null && maxSubmissions > 0;

        if (hasLimit) {
            return submitWithLimit(request, template, userId, username, httpRequest, maxSubmissions);
        } else {
            return submitWithoutLimit(request, template, userId, username, httpRequest);
        }
    }

    /**
     * 有提交限制的提交（使用原子计数器 + 问卷级别锁）
     * 修改提交不会增加计数器
     */
    private Map<String, Object> submitWithLimit(SubmitRequest request, SurveyTemplate template,
                                                 String userId, String username, HttpServletRequest httpRequest,
                                                 int maxSubmissions) {
        String templateUuid = request.getTemplateUuid();
        String templateId = template.getId();

        // 先检查用户是否已提交过（修改提交不占用新份数）
        SurveyAnswer existingAnswer = null;
        if (userId != null) {
            existingAnswer = answerRepository.selectOne(
                    new LambdaQueryWrapper<SurveyAnswer>()
                            .eq(SurveyAnswer::getTemplateId, templateId)
                            .eq(SurveyAnswer::getUserId, userId)
                            .orderByDesc(SurveyAnswer::getCreateTime)
                            .last("LIMIT 1")
            );
        }

        // 如果是修改提交，直接执行更新逻辑，不占用新份数
        if (existingAnswer != null) {
            if (!template.getAllowEdit().equals(1)) {
                throw new BusinessException(ErrorCode.SURVEY_ALREADY_SUBMITTED, "该问卷不允许修改");
            }
            log.debug("用户已提交过，执行修改提交: userId={}, templateUuid={}", userId, templateUuid);
            return doSubmitWithExistingAnswer(request, template, existingAnswer, username, httpRequest);
        }

        // 新提交：需要检查份数限制
        // 快速预检查：读取Redis计数器
        Long currentCount = redisUtils.getSubmissionCount(templateUuid);
        if (currentCount == null) {
            // 计数器不存在，从数据库加载并初始化
            currentCount = templateRepository.countSubmissions(templateId);
            redisUtils.initSubmissionCount(templateUuid, currentCount, 86400); // 24小时过期
            log.debug("初始化提交计数器: templateUuid={}, count={}", templateUuid, currentCount);
        }

        // 如果已经超限，直接返回（无需竞争锁）
        if (currentCount >= maxSubmissions) {
            log.warn("提交已达上限: templateUuid={}, current={}, max={}", templateUuid, currentCount, maxSubmissions);
            throw new BusinessException(ErrorCode.SUBMISSION_LIMIT_REACHED,
                    String.format("问卷提交次数已达上限（%d/%d）", maxSubmissions, maxSubmissions));
        }

        // 获取问卷级别提交锁
        String lockValue = UUID.randomUUID().toString();
        if (!redisUtils.trySubmissionLock(templateUuid, lockValue, 10)) {
            log.warn("获取问卷提交锁失败: templateUuid={}", templateUuid);
            throw new BusinessException(ErrorCode.SUBMISSION_IN_PROGRESS,
                    "当前问卷正在处理其他提交，请稍后重试");
        }

        try {
            // 原子递增计数器并检查
            if (!redisUtils.incrementSubmissionCount(templateUuid, maxSubmissions, 86400)) {
                log.warn("原子递增失败，已达上限: templateUuid={}", templateUuid);
                throw new BusinessException(ErrorCode.SUBMISSION_LIMIT_REACHED,
                        String.format("问卷提交次数已达上限（%d/%d）", maxSubmissions, maxSubmissions));
            }

            log.debug("原子递增成功，开始执行业务逻辑: templateUuid={}", templateUuid);

            // 执行新建提交逻辑
            return doSubmitNewAnswer(request, template, userId, username, httpRequest);

        } catch (Exception e) {
            // 发生异常，回滚计数器
            log.error("提交失败，回滚计数器: templateUuid={}, error={}", templateUuid, e.getMessage());
            redisUtils.decrementSubmissionCount(templateUuid);
            throw e;
        } finally {
            // 释放问卷级别锁
            redisUtils.releaseSubmissionLock(templateUuid, lockValue);
        }
    }

    /**
     * 无提交限制的提交（使用用户级别防重复锁）
     */
    private Map<String, Object> submitWithoutLimit(SubmitRequest request, SurveyTemplate template,
                                                    String userId, String username, HttpServletRequest httpRequest) {
        String templateUuid = request.getTemplateUuid();

        // 用户级别防重复锁
        String userKey = userId != null ? userId : "anon_" + IpUtils.getClientIp(httpRequest);
        String lockKey = "submit:" + userKey + ":" + templateUuid;
        String lockValue = UUID.randomUUID().toString();

        if (!redisUtils.tryLock(lockKey, lockValue, 30)) {
            throw new BusinessException(ErrorCode.SUBMISSION_IN_PROGRESS);
        }

        try {
            return doSubmit(request, template, userId, username, httpRequest);
        } finally {
            redisUtils.releaseLock(lockKey, lockValue);
        }
    }

    /**
     * 执行修改提交（更新现有答案，不增加计数器）
     */
    private Map<String, Object> doSubmitWithExistingAnswer(SubmitRequest request, SurveyTemplate template,
                                                            SurveyAnswer existingAnswer, String username,
                                                            HttpServletRequest httpRequest) {
        String templateUuid = request.getTemplateUuid();

        // 计算分数
        BigDecimal totalScore = scoringService.calculateScore(template.getSchemaJson(), request.getData());

        // 更新现有答案
        existingAnswer.setAnswerData(request.getData());
        existingAnswer.setTotalScore(totalScore);
        existingAnswer.setSubmitIp(IpUtils.getClientIp(httpRequest));
        existingAnswer.setUserAgent(httpRequest.getHeader("User-Agent"));
        existingAnswer.setDurationSeconds(request.getDurationSeconds());
        existingAnswer.setSubmitterName(username != null ? username : "匿名用户");
        answerRepository.updateById(existingAnswer);

        log.info("问卷修改提交成功: answerUuid={}, templateUuid={}, score={}", existingAnswer.getId(), templateUuid, totalScore);

        return Map.of(
                "answerUuid", existingAnswer.getId(),
                "totalScore", totalScore,
                "isUpdate", true
        );
    }

    /**
     * 执行新建提交（创建新答案）
     */
    private Map<String, Object> doSubmitNewAnswer(SubmitRequest request, SurveyTemplate template,
                                                   String userId, String username, HttpServletRequest httpRequest) {
        String templateUuid = request.getTemplateUuid();
        String templateId = template.getId();

        // 计算分数
        BigDecimal totalScore = scoringService.calculateScore(template.getSchemaJson(), request.getData());

        // 创建新答案
        SurveyAnswer answer = new SurveyAnswer();
        answer.setId(UUID.randomUUID().toString().replace("-", ""));
        answer.setTemplateId(templateId);
        answer.setUserId(userId);
        answer.setSubmitterName(username != null ? username : "匿名用户");
        answer.setAnswerData(request.getData());
        answer.setTotalScore(totalScore);
        answer.setSubmitIp(IpUtils.getClientIp(httpRequest));
        answer.setUserAgent(httpRequest.getHeader("User-Agent"));
        answer.setDurationSeconds(request.getDurationSeconds());
        answerRepository.insert(answer);

        // 发送异步事件
        SubmissionEvent event = SubmissionEvent.builder()
                .answerUuid(answer.getId())
                .templateUuid(templateUuid)
                .userId(userId)
                .submitterName(username)
                .totalScore(totalScore)
                .timestamp(System.currentTimeMillis())
                .notifyAdmin(false)
                .build();

        submissionProducer.sendSubmissionEvent(event);

        log.info("问卷新建提交成功: answerUuid={}, templateUuid={}, score={}", answer.getId(), templateUuid, totalScore);

        return Map.of(
                "answerUuid", answer.getId(),
                "totalScore", totalScore,
                "isUpdate", false
        );
    }

    /**
     * 执行实际的提交逻辑
     */
    private Map<String, Object> doSubmit(SubmitRequest request, SurveyTemplate template,
                                          String userId, String username, HttpServletRequest httpRequest) {
        String templateUuid = request.getTemplateUuid();
        String templateId = template.getId();

        // 检查是否已提交
        SurveyAnswer existingAnswer = null;
        if (userId != null) {
            existingAnswer = answerRepository.selectOne(
                    new LambdaQueryWrapper<SurveyAnswer>()
                            .eq(SurveyAnswer::getTemplateId, templateId)
                            .eq(SurveyAnswer::getUserId, userId)
                            .orderByDesc(SurveyAnswer::getCreateTime)
                            .last("LIMIT 1")
            );
        }

        // 如果已提交且不允许修改，抛出异常
        if (existingAnswer != null && !template.getAllowEdit().equals(1)) {
            throw new BusinessException(ErrorCode.SURVEY_ALREADY_SUBMITTED);
        }

        // 计算分数
        BigDecimal totalScore = scoringService.calculateScore(template.getSchemaJson(), request.getData());

        SurveyAnswer answer;
        boolean isUpdate = false;

        if (existingAnswer != null) {
            // 更新现有答案
            answer = existingAnswer;
            answer.setAnswerData(request.getData());
            answer.setTotalScore(totalScore);
            answer.setSubmitIp(IpUtils.getClientIp(httpRequest));
            answer.setUserAgent(httpRequest.getHeader("User-Agent"));
            answer.setDurationSeconds(request.getDurationSeconds());
            answer.setSubmitterName(username != null ? username : "匿名用户");
            answerRepository.updateById(answer);
            isUpdate = true;
            log.info("问卷修改成功: answerUuid={}, templateUuid={}, score={}", answer.getId(), templateUuid, totalScore);
        } else {
            // 新建答案
            answer = new SurveyAnswer();
            answer.setId(UUID.randomUUID().toString().replace("-", ""));
            answer.setTemplateId(templateId);
            answer.setUserId(userId);
            answer.setSubmitterName(username != null ? username : "匿名用户");
            answer.setAnswerData(request.getData());
            answer.setTotalScore(totalScore);
            answer.setSubmitIp(IpUtils.getClientIp(httpRequest));
            answer.setUserAgent(httpRequest.getHeader("User-Agent"));
            answer.setDurationSeconds(request.getDurationSeconds());
            answerRepository.insert(answer);
            log.info("问卷提交成功: answerUuid={}, templateUuid={}, score={}", answer.getId(), templateUuid, totalScore);
        }

        // 发送异步事件 (仅新建时发送)
        if (!isUpdate) {
            SubmissionEvent event = SubmissionEvent.builder()
                    .answerUuid(answer.getId())
                    .templateUuid(templateUuid)
                    .userId(userId)
                    .submitterName(username)
                    .totalScore(totalScore)
                    .timestamp(System.currentTimeMillis())
                    .notifyAdmin(false)
                    .build();

            submissionProducer.sendSubmissionEvent(event);
        }

        return Map.of(
                "answerUuid", answer.getId(),
                "totalScore", totalScore,
                "isUpdate", isUpdate
        );
    }

    /**
     * 获取用户在某问卷的提交结果
     */
    public Map<String, Object> getUserSubmissionResult(String templateUuid, String userId) {
        SurveyTemplate template = templateRepository.findByUuid(templateUuid);
        if (template == null) {
            throw new BusinessException(ErrorCode.SURVEY_NOT_FOUND);
        }

        log.debug("查询提交结果: templateId={}, userId={}", template.getId(), userId);

        SurveyAnswer answer = answerRepository.selectOne(
                new LambdaQueryWrapper<SurveyAnswer>()
                        .eq(SurveyAnswer::getTemplateId, template.getId())
                        .eq(SurveyAnswer::getUserId, userId)
                        .orderByDesc(SurveyAnswer::getCreateTime)
                        .last("LIMIT 1")
        );

        log.debug("查询结果: answer={}", answer != null ? "found, id=" + answer.getId() : "not found");

        if (answer == null) {
            BigDecimal maxScore = scoringService.getMaxScore(template.getSchemaJson());
            return Map.of(
                    "totalScore", BigDecimal.ZERO,
                    "maxScore", maxScore != null ? maxScore : BigDecimal.ZERO,
                    "answers", List.of(),
                    "showScore", template.getShowScore() != null && template.getShowScore() == 1
            );
        }

        // 解析答案详情
        List<Map<String, Object>> answers = scoringService.getAnswerDetails(
                template.getSchemaJson(),
                answer.getAnswerData()
        );

        log.debug("答案详情: answers size={}", answers.size());

        BigDecimal maxScore = scoringService.getMaxScore(template.getSchemaJson());
        BigDecimal totalScore = answer.getTotalScore() != null ? answer.getTotalScore() : BigDecimal.ZERO;

        Map<String, Object> result = new HashMap<>();
        result.put("totalScore", totalScore);
        result.put("maxScore", maxScore != null ? maxScore : BigDecimal.ZERO);
        result.put("answers", answers);
        result.put("submitTime", answer.getCreateTime() != null ? answer.getCreateTime() : "");
        result.put("showScore", template.getShowScore() != null && template.getShowScore() == 1);

        return result;
    }

    /**
     * 重新提交 (删除旧答案)
     */
    @Transactional
    public void deletePreviousAnswer(String templateUuid, String userId) {
        SurveyTemplate template = templateRepository.findByUuid(templateUuid);
        if (template == null) {
            throw new BusinessException(ErrorCode.SURVEY_NOT_FOUND);
        }

        if (!template.getAllowEdit().equals(1)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "该问卷不允许修改");
        }

        // 删除旧答案
        answerRepository.delete(
                new LambdaQueryWrapper<SurveyAnswer>()
                        .eq(SurveyAnswer::getTemplateId, template.getId())
                        .eq(SurveyAnswer::getUserId, userId)
        );

        log.info("删除旧答案: templateUuid={}, userId={}", templateUuid, userId);
    }
}