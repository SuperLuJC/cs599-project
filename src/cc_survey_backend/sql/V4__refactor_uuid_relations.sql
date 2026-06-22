-- ============================================
-- CC_SURVEY 数据库重构脚本
-- 版本: V4
-- 描述: 重构表结构，使用uuid作为关联字段，展示字段使用username
-- ============================================

USE `cc_survey_db`;

-- ============================================
-- 1. 修改问卷模板表
-- ============================================
-- 添加创建人UUID和创建人用户名字段
ALTER TABLE `survey_template`
ADD COLUMN `created_by_uuid` VARCHAR(32) COMMENT '创建人UUID' AFTER `created_by`,
ADD COLUMN `created_by_name` VARCHAR(50) COMMENT '创建人用户名' AFTER `created_by_uuid`;

-- 迁移数据：根据created_by填充created_by_uuid和created_by_name
UPDATE `survey_template` t
JOIN `sys_user` u ON t.created_by = u.id
SET t.created_by_uuid = u.uuid,
    t.created_by_name = u.username
WHERE t.created_by IS NOT NULL;

-- ============================================
-- 2. 修改问卷答卷表
-- ============================================
-- 添加提交人用户名字段（如果不存在）
-- 注意：submitter_name字段已存在，用于存储真实姓名

-- 确保user_uuid字段正确填充
UPDATE `survey_answer` a
JOIN `sys_user` u ON a.user_id = u.id
SET a.user_uuid = u.uuid
WHERE a.user_id IS NOT NULL AND (a.user_uuid IS NULL OR a.user_uuid = '');

-- ============================================
-- 3. 修改文件表
-- ============================================
-- 添加上传人UUID和用户名字段
ALTER TABLE `sys_file`
ADD COLUMN `upload_user_uuid` VARCHAR(32) COMMENT '上传人UUID' AFTER `upload_user_id`,
ADD COLUMN `upload_user_name` VARCHAR(50) COMMENT '上传人用户名' AFTER `upload_user_uuid`;

-- 迁移数据
UPDATE `sys_file` f
JOIN `sys_user` u ON f.upload_user_id = u.id
SET f.upload_user_uuid = u.uuid,
    f.upload_user_name = u.username
WHERE f.upload_user_id IS NOT NULL;

-- ============================================
-- 4. 修改操作日志表
-- ============================================
-- 添加操作人UUID字段
ALTER TABLE `sys_oper_log`
ADD COLUMN `oper_user_uuid` VARCHAR(32) COMMENT '操作人UUID' AFTER `oper_user_id`;

-- 迁移数据
UPDATE `sys_oper_log` l
JOIN `sys_user` u ON l.oper_user_id = u.id
SET l.oper_user_uuid = u.uuid
WHERE l.oper_user_id IS NOT NULL;

-- ============================================
-- 5. 添加新索引
-- ============================================
-- 问卷模板表：按创建人UUID查询
CREATE INDEX `idx_template_creator_uuid` ON `survey_template` (`created_by_uuid`);

-- 问卷答卷表：按用户UUID查询
CREATE INDEX `idx_answer_user_uuid` ON `survey_answer` (`user_uuid`);

-- 文件表：按上传人UUID查询
CREATE INDEX `idx_file_upload_user_uuid` ON `sys_file` (`upload_user_uuid`);

-- 操作日志表：按操作人UUID查询
CREATE INDEX `idx_log_oper_user_uuid` ON `sys_oper_log` (`oper_user_uuid`);

-- ============================================
-- 6. 更新统计视图
-- ============================================
DROP VIEW IF EXISTS `v_survey_stats`;
CREATE OR REPLACE VIEW `v_survey_stats` AS
SELECT
    t.id,
    t.uuid,
    t.title,
    t.status,
    t.create_time,
    t.created_by_uuid,
    t.created_by_name,
    COUNT(DISTINCT a.id) AS submission_count,
    AVG(a.total_score) AS avg_score,
    MAX(a.total_score) AS max_score,
    MIN(a.total_score) AS min_score
FROM `survey_template` t
LEFT JOIN `survey_answer` a ON t.id = a.template_id AND a.deleted = 0
WHERE t.deleted = 0
GROUP BY t.id, t.uuid, t.title, t.status, t.create_time, t.created_by_uuid, t.created_by_name;

DROP VIEW IF EXISTS `v_user_stats`;
CREATE OR REPLACE VIEW `v_user_stats` AS
SELECT
    u.id,
    u.uuid,
    u.username,
    u.name,
    u.role,
    u.status,
    COUNT(DISTINCT a.id) AS submission_count,
    MAX(a.create_time) AS last_submission_time
FROM `sys_user` u
LEFT JOIN `survey_answer` a ON u.uuid = a.user_uuid AND a.deleted = 0
WHERE u.deleted = 0
GROUP BY u.id, u.uuid, u.username, u.name, u.role, u.status;
