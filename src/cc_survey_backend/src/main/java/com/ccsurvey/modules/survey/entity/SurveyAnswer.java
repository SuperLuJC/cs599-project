package com.ccsurvey.modules.survey.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 问卷答卷实体
 */
@Data
@TableName(value = "survey_answer", autoResultMap = true)
public class SurveyAnswer {

    /**
     * UUID主键 (32位无横线)
     */
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /**
     * 问卷模板UUID
     */
    private String templateId;

    /**
     * 用户UUID (匿名时为空)
     */
    private String userId;

    /**
     * 提交人用户名 (展示用)
     */
    private String submitterName;

    /**
     * 答案数据
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> answerData;

    /**
     * 总分
     */
    private BigDecimal totalScore;

    /**
     * 提交IP
     */
    private String submitIp;

    /**
     * 浏览器UA
     */
    private String userAgent;

    /**
     * 填写耗时(秒)
     */
    private Integer durationSeconds;

    /**
     * 提交时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 逻辑删除
     */
    @TableLogic
    private Integer deleted;
}
