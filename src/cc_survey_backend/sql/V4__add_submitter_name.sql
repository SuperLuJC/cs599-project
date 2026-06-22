-- ============================================
-- CC_SURVEY 数据库迁移脚本
-- 版本: V4
-- 描述: 添加答卷提交人姓名字段
-- ============================================

USE `cc_survey_db`;

-- 添加 submitter_name 字段到 survey_answer 表
ALTER TABLE `survey_answer`
ADD COLUMN `submitter_name` VARCHAR(50) COMMENT '提交人姓名' AFTER `user_uuid`;
