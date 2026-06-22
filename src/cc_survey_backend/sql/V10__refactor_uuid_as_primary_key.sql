-- ============================================
-- CC_SURVEY 数据库重构脚本
-- 版本: V10
-- 描述: 将id和uuid合并，使用uuid作为主键
-- ============================================

-- 注意：此脚本会删除原有数据，请在执行前备份数据
-- 外键约束需要先手动删除（已删除）

USE `cc_survey_db`;

-- ============================================
-- 1. 用户表重构
-- ============================================
-- 备份数据
DROP TABLE IF EXISTS `sys_user_backup`;
CREATE TABLE `sys_user_backup` AS SELECT * FROM `sys_user`;

-- 删除旧表并重建
DROP TABLE IF EXISTS `sys_user`;

CREATE TABLE `sys_user` (
    `id` VARCHAR(32) PRIMARY KEY COMMENT 'UUID主键',
    `username` VARCHAR(50) NOT NULL UNIQUE COMMENT '登录账号',
    `password` VARCHAR(255) NOT NULL COMMENT 'BCrypt加密密码',
    `email` VARCHAR(100) UNIQUE COMMENT '邮箱',
    `phone` VARCHAR(20) COMMENT '手机号',
    `name` VARCHAR(50) COMMENT '真实姓名',
    `avatar` VARCHAR(500) COMMENT '头像URL',
    `role` VARCHAR(20) NOT NULL DEFAULT 'user' COMMENT '角色: admin, user',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1-正常, 0-禁用',
    `email_verified` TINYINT DEFAULT 0 COMMENT '邮箱是否已验证',
    `last_login_time` DATETIME COMMENT '最后登录时间',
    `login_fail_count` INT DEFAULT 0 COMMENT '登录失败次数',
    `locked_until` DATETIME COMMENT '账户锁定截止时间',
    `password_reset_token` VARCHAR(100) COMMENT '密码重置令牌',
    `password_reset_expires` DATETIME COMMENT '重置令牌过期时间',
    `email_verify_token` VARCHAR(100) COMMENT '邮箱验证令牌',
    `email_verify_expires` DATETIME COMMENT '验证令牌过期时间',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',

    INDEX `idx_username` (`username`),
    INDEX `idx_email` (`email`),
    INDEX `idx_status` (`status`),
    INDEX `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统用户表';

-- 从备份恢复数据（将原uuid映射到新id）
INSERT INTO `sys_user` (`id`, `username`, `password`, `email`, `phone`, `name`, `avatar`, `role`, `status`, `email_verified`, `last_login_time`, `login_fail_count`, `locked_until`, `password_reset_token`, `password_reset_expires`, `email_verify_token`, `email_verify_expires`, `create_time`, `update_time`, `deleted`)
SELECT `uuid`, `username`, `password`, `email`, `phone`, `name`, `avatar`, `role`, `status`, `email_verified`, `last_login_time`, `login_fail_count`, `locked_until`, `password_reset_token`, `password_reset_expires`, `email_verify_token`, `email_verify_expires`, `create_time`, `update_time`, `deleted`
FROM `sys_user_backup`;

-- ============================================
-- 2. 问卷模板表重构
-- ============================================
DROP TABLE IF EXISTS `survey_template_backup`;
CREATE TABLE `survey_template_backup` AS SELECT * FROM `survey_template`;

DROP TABLE IF EXISTS `survey_template`;

CREATE TABLE `survey_template` (
    `id` VARCHAR(32) PRIMARY KEY COMMENT 'UUID主键',
    `title` VARCHAR(200) NOT NULL COMMENT '问卷标题',
    `description` TEXT COMMENT '问卷描述',
    `form_id` VARCHAR(50) UNIQUE COMMENT '业务标识符',
    `schema_json` JSON NOT NULL COMMENT '表单JSON Schema',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态: 0-草稿, 1-已发布, 2-已归档',
    `version` INT NOT NULL DEFAULT 1 COMMENT '版本号',
    `start_time` DATETIME COMMENT '开始时间',
    `end_time` DATETIME COMMENT '结束时间',
    `max_submissions` INT DEFAULT 0 COMMENT '最大提交数',
    `allow_edit` TINYINT DEFAULT 0 COMMENT '是否允许修改',
    `allow_anonymous` TINYINT DEFAULT 0 COMMENT '是否允许匿名',
    `created_by` VARCHAR(32) COMMENT '创建人UUID',
    `created_by_name` VARCHAR(50) COMMENT '创建人用户名',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `publish_time` DATETIME COMMENT '发布时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',

    INDEX `idx_form_id` (`form_id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_created_by` (`created_by`),
    INDEX `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='问卷模板表';

-- 从备份恢复数据，将created_by从原id映射到原uuid
INSERT INTO `survey_template` (`id`, `title`, `description`, `form_id`, `schema_json`, `status`, `version`, `start_time`, `end_time`, `max_submissions`, `allow_edit`, `allow_anonymous`, `created_by`, `created_by_name`, `create_time`, `update_time`, `publish_time`, `deleted`)
SELECT t.`uuid`, t.`title`, t.`description`, t.`form_id`, t.`schema_json`, t.`status`, t.`version`, t.`start_time`, t.`end_time`, t.`max_submissions`, t.`allow_edit`, t.`allow_anonymous`, u.`uuid`, u.`name`, t.`create_time`, t.`update_time`, t.`publish_time`, t.`deleted`
FROM `survey_template_backup` t
LEFT JOIN `sys_user_backup` u ON t.`created_by` = u.`id`;

-- ============================================
-- 3. 问卷答卷表重构
-- ============================================
DROP TABLE IF EXISTS `survey_answer_backup`;
CREATE TABLE `survey_answer_backup` AS SELECT * FROM `survey_answer`;

DROP TABLE IF EXISTS `survey_answer`;

CREATE TABLE `survey_answer` (
    `id` VARCHAR(32) PRIMARY KEY COMMENT 'UUID主键',
    `template_id` VARCHAR(32) NOT NULL COMMENT '问卷模板UUID',
    `user_id` VARCHAR(32) COMMENT '用户UUID，匿名时为空',
    `submitter_name` VARCHAR(50) COMMENT '提交人用户名',
    `answer_data` JSON NOT NULL COMMENT '答案数据',
    `total_score` DECIMAL(10, 3) DEFAULT 0 COMMENT '总分',
    `submit_ip` VARCHAR(50) COMMENT '提交IP',
    `user_agent` VARCHAR(500) COMMENT '浏览器UA',
    `duration_seconds` INT COMMENT '填写耗时(秒)',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',

    INDEX `idx_template_id` (`template_id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='问卷答卷表';

-- 从备份恢复数据
INSERT INTO `survey_answer` (`id`, `template_id`, `user_id`, `submitter_name`, `answer_data`, `total_score`, `submit_ip`, `user_agent`, `duration_seconds`, `create_time`, `deleted`)
SELECT a.`uuid`, t.`uuid`, u.`uuid`, COALESCE(a.`submitter_name`, u.`name`), a.`answer_data`, a.`total_score`, a.`submit_ip`, a.`user_agent`, a.`duration_seconds`, a.`create_time`, a.`deleted`
FROM `survey_answer_backup` a
LEFT JOIN `survey_template_backup` t ON a.`template_id` = t.`id`
LEFT JOIN `sys_user_backup` u ON a.`user_id` = u.`id`;

-- ============================================
-- 4. 文件元数据表重构
-- ============================================
DROP TABLE IF EXISTS `sys_file_backup`;
CREATE TABLE `sys_file_backup` AS SELECT * FROM `sys_file`;

DROP TABLE IF EXISTS `sys_file`;

CREATE TABLE `sys_file` (
    `id` VARCHAR(32) PRIMARY KEY COMMENT 'UUID主键',
    `original_name` VARCHAR(255) NOT NULL COMMENT '原始文件名',
    `stored_name` VARCHAR(255) NOT NULL COMMENT '存储文件名',
    `file_path` VARCHAR(500) NOT NULL COMMENT '文件路径',
    `file_url` VARCHAR(500) COMMENT '访问URL',
    `file_size` BIGINT NOT NULL COMMENT '文件大小(字节)',
    `file_type` VARCHAR(50) COMMENT 'MIME类型',
    `file_ext` VARCHAR(20) COMMENT '文件扩展名',
    `file_hash` VARCHAR(64) COMMENT 'SHA-256哈希',
    `upload_user_id` VARCHAR(32) COMMENT '上传用户UUID',
    `upload_user_name` VARCHAR(50) COMMENT '上传用户名',
    `related_type` VARCHAR(50) COMMENT '关联类型',
    `related_id` VARCHAR(32) COMMENT '关联ID',
    `status` TINYINT DEFAULT 1 COMMENT '状态',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',

    INDEX `idx_file_hash` (`file_hash`),
    INDEX `idx_upload_user_id` (`upload_user_id`),
    INDEX `idx_related` (`related_type`, `related_id`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文件元数据表';

-- 从备份恢复数据
INSERT INTO `sys_file` (`id`, `original_name`, `stored_name`, `file_path`, `file_url`, `file_size`, `file_type`, `file_ext`, `file_hash`, `upload_user_id`, `upload_user_name`, `related_type`, `related_id`, `status`, `create_time`)
SELECT f.`uuid`, f.`original_name`, f.`stored_name`, f.`file_path`, f.`file_url`, f.`file_size`, f.`file_type`, f.`file_ext`, f.`file_hash`, u.`uuid`, u.`name`, f.`related_type`, f.`related_id`, f.`status`, f.`create_time`
FROM `sys_file_backup` f
LEFT JOIN `sys_user_backup` u ON f.`upload_user_id` = u.`id`;

-- ============================================
-- 5. 操作日志表重构
-- ============================================
DROP TABLE IF EXISTS `sys_oper_log_backup`;
CREATE TABLE `sys_oper_log_backup` AS SELECT * FROM `sys_oper_log`;

DROP TABLE IF EXISTS `sys_oper_log`;

CREATE TABLE `sys_oper_log` (
    `id` VARCHAR(32) PRIMARY KEY COMMENT 'UUID主键',
    `trace_id` VARCHAR(32) NOT NULL COMMENT '追踪ID',
    `title` VARCHAR(100) DEFAULT '' COMMENT '操作标题',
    `business_type` VARCHAR(20) DEFAULT 'OTHER' COMMENT '业务类型',
    `method` VARCHAR(200) DEFAULT '' COMMENT '方法名称',
    `request_method` VARCHAR(10) DEFAULT '' COMMENT '请求方式',
    `oper_user_id` VARCHAR(32) COMMENT '操作用户UUID',
    `oper_name` VARCHAR(50) COMMENT '操作用户名',
    `oper_url` VARCHAR(255) COMMENT '请求URL',
    `oper_ip` VARCHAR(50) COMMENT '操作IP',
    `oper_location` VARCHAR(100) COMMENT '操作地点',
    `oper_param` TEXT COMMENT '请求参数',
    `json_result` LONGTEXT COMMENT '返回结果',
    `status` TINYINT DEFAULT 0 COMMENT '状态',
    `error_msg` TEXT COMMENT '错误信息',
    `cost_time` BIGINT DEFAULT 0 COMMENT '耗时(毫秒)',
    `oper_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',

    INDEX `idx_trace_id` (`trace_id`),
    INDEX `idx_oper_user_id` (`oper_user_id`),
    INDEX `idx_oper_time` (`oper_time`),
    INDEX `idx_business_type` (`business_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志表';

-- 从备份恢复数据
INSERT INTO `sys_oper_log` (`id`, `trace_id`, `title`, `business_type`, `method`, `request_method`, `oper_user_id`, `oper_name`, `oper_url`, `oper_ip`, `oper_location`, `oper_param`, `json_result`, `status`, `error_msg`, `cost_time`, `oper_time`)
SELECT REPLACE(UUID(), '-', ''), `trace_id`, `title`, `business_type`, `method`, `request_method`,
       (SELECT `uuid` FROM `sys_user_backup` WHERE `id` = `oper_user_id`),
       `oper_name`, `oper_url`, `oper_ip`, `oper_location`, `oper_param`, `json_result`, `status`, `error_msg`, `cost_time`, `oper_time`
FROM `sys_oper_log_backup`;

-- ============================================
-- 6. 邮件验证码表重构
-- ============================================
DROP TABLE IF EXISTS `sys_email_code_backup`;
CREATE TABLE `sys_email_code_backup` AS SELECT * FROM `sys_email_code`;

DROP TABLE IF EXISTS `sys_email_code`;

CREATE TABLE `sys_email_code` (
    `id` VARCHAR(32) PRIMARY KEY COMMENT 'UUID主键',
    `email` VARCHAR(100) NOT NULL COMMENT '邮箱地址',
    `code` VARCHAR(10) NOT NULL COMMENT '验证码',
    `type` VARCHAR(20) NOT NULL COMMENT '类型',
    `used` TINYINT DEFAULT 0 COMMENT '是否已使用',
    `expire_time` DATETIME NOT NULL COMMENT '过期时间',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    INDEX `idx_email_type` (`email`, `type`),
    INDEX `idx_expire_time` (`expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='邮件验证码表';

-- 从备份恢复数据，生成新的UUID
INSERT INTO `sys_email_code` (`id`, `email`, `code`, `type`, `used`, `expire_time`, `create_time`)
SELECT REPLACE(UUID(), '-', ''), `email`, `code`, `type`, `used`, `expire_time`, `create_time`
FROM `sys_email_code_backup`;

-- ============================================
-- 7. 系统配置表重构
-- ============================================
DROP TABLE IF EXISTS `sys_config_backup`;
CREATE TABLE `sys_config_backup` AS SELECT * FROM `sys_config`;

DROP TABLE IF EXISTS `sys_config`;

CREATE TABLE `sys_config` (
    `id` VARCHAR(32) PRIMARY KEY COMMENT 'UUID主键',
    `config_key` VARCHAR(100) NOT NULL UNIQUE COMMENT '配置键',
    `config_value` TEXT COMMENT '配置值',
    `config_type` VARCHAR(50) DEFAULT 'STRING' COMMENT '类型',
    `description` VARCHAR(255) COMMENT '配置描述',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX `idx_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统配置表';

-- 从备份恢复数据
INSERT INTO `sys_config` (`id`, `config_key`, `config_value`, `config_type`, `description`, `create_time`, `update_time`)
SELECT REPLACE(UUID(), '-', ''), `config_key`, `config_value`, `config_type`, `description`, `create_time`, `update_time`
FROM `sys_config_backup`;

SELECT '数据库重构完成!' AS message;