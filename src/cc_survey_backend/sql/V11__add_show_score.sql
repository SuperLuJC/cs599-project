-- ============================================
-- CC_SURVEY 数据库迁移脚本
-- 版本: V11
-- 描述: 添加问卷评分展示设置
-- ============================================

-- 添加 show_score 字段到 survey_template 表
ALTER TABLE `survey_template`
ADD COLUMN `show_score` TINYINT DEFAULT 0 COMMENT '是否向用户展示评分: 0-不展示, 1-展示'
AFTER `allow_anonymous`;
