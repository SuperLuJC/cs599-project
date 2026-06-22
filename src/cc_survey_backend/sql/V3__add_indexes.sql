-- ============================================
-- CC_SURVEY 数据库索引优化脚本
-- 版本: V3
-- 描述: 添加复合索引和优化查询性能
-- ============================================

USE `cc_survey_db`;

-- ============================================
-- 复合索引优化
-- ============================================

-- 用户表：按状态和创建时间查询
CREATE INDEX `idx_user_status_createtime` ON `sys_user` (`status`, `create_time`);

-- 问卷模板表：按状态和创建时间查询
CREATE INDEX `idx_template_status_createtime` ON `survey_template` (`status`, `create_time`);

-- 问卷模板表：按创建人和状态查询
CREATE INDEX `idx_template_creator_status` ON `survey_template` (`created_by`, `status`);

-- 问卷答卷表：按模板和时间范围查询
CREATE INDEX `idx_answer_template_time` ON `survey_answer` (`template_id`, `create_time`);

-- 问卷答卷表：按用户和时间查询
CREATE INDEX `idx_answer_user_time` ON `survey_answer` (`user_id`, `create_time`);

-- 操作日志表：按用户和时间查询
CREATE INDEX `idx_log_user_time` ON `sys_oper_log` (`oper_user_id`, `oper_time`);

-- 操作日志表：按类型和时间查询
CREATE INDEX `idx_log_type_time` ON `sys_oper_log` (`business_type`, `oper_time`);

-- 文件表：按状态和创建时间清理临时文件
CREATE INDEX `idx_file_status_createtime` ON `sys_file` (`status`, `create_time`);

-- ============================================
-- 全文索引 (如果需要搜索功能)
-- ============================================

-- 问卷标题全文索引
ALTER TABLE `survey_template` ADD FULLTEXT INDEX `ft_title` (`title`);

-- ============================================
-- 统计视图 (可选，用于统计仪表盘)
-- ============================================

-- 问卷统计视图
CREATE OR REPLACE VIEW `v_survey_stats` AS
SELECT
    t.id,
    t.uuid,
    t.title,
    t.status,
    t.create_time,
    COUNT(DISTINCT a.id) AS submission_count,
    AVG(a.total_score) AS avg_score,
    MAX(a.total_score) AS max_score,
    MIN(a.total_score) AS min_score
FROM `survey_template` t
LEFT JOIN `survey_answer` a ON t.id = a.template_id AND a.deleted = 0
WHERE t.deleted = 0
GROUP BY t.id, t.uuid, t.title, t.status, t.create_time;

-- 用户统计视图
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
LEFT JOIN `survey_answer` a ON u.id = a.user_id AND a.deleted = 0
WHERE u.deleted = 0
GROUP BY u.id, u.uuid, u.username, u.name, u.role, u.status;
