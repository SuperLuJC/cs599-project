package com.ccsurvey.modules.survey.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 问卷模板实体
 */
@Data
@TableName(value = "survey_template", autoResultMap = true)
public class SurveyTemplate {

    /**
     * UUID主键 (32位无横线)
     */
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /**
     * 问卷标题
     */
    private String title;

    /**
     * 问卷描述
     */
    private String description;

    /**
     * 业务标识符
     */
    private String formId;

    /**
     * 表单JSON Schema
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> schemaJson;

    /**
     * 状态: 0-草稿, 1-已发布, 2-已归档
     */
    private Integer status;

    /**
     * 版本号
     */
    private Integer version;

    /**
     * 开始时间
     */
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    private LocalDateTime endTime;

    /**
     * 最大提交数 (0表示不限制)
     */
    private Integer maxSubmissions;

    /**
     * 是否允许修改
     */
    private Integer allowEdit;

    /**
     * 是否允许匿名
     */
    private Integer allowAnonymous;

    /**
     * 是否向用户展示评分 (0-不展示, 1-展示)
     */
    private Integer showScore;

    /**
     * 创建人UUID
     */
    private String createdBy;

    /**
     * 创建人用户名 (展示用)
     */
    private String createdByName;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 发布时间
     */
    private LocalDateTime publishTime;

    /**
     * 逻辑删除
     */
    @TableLogic
    private Integer deleted;

    // ==================== 业务方法 ====================

    /**
     * 是否为草稿
     */
    public boolean isDraft() {
        return Integer.valueOf(0).equals(this.status);
    }

    /**
     * 是否已发布
     */
    public boolean isPublished() {
        return Integer.valueOf(1).equals(this.status);
    }

    /**
     * 是否已归档
     */
    public boolean isArchived() {
        return Integer.valueOf(2).equals(this.status);
    }

    /**
     * 是否在有效期内
     */
    public boolean isValidPeriod() {
        LocalDateTime now = LocalDateTime.now();
        if (startTime != null && now.isBefore(startTime)) {
            return false;
        }
        if (endTime != null && now.isAfter(endTime)) {
            return false;
        }
        return true;
    }

    /**
     * 是否已过期
     */
    public boolean isExpired() {
        return endTime != null && LocalDateTime.now().isAfter(endTime);
    }

    /**
     * 是否未开始
     */
    public boolean isNotStarted() {
        if (startTime == null) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        boolean notStarted = now.isBefore(startTime);
        // 添加日志便于调试
        if (notStarted) {
            System.out.println("[DEBUG] 问卷未开始: startTime=" + startTime + ", now=" + now);
        }
        return notStarted;
    }
}
