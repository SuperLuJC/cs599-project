-- ============================================
-- CC_SURVEY 数据库初始化脚本
-- 版本: V1
-- 描述: 创建基础表结构
-- ============================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS `cc_survey_db` DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `cc_survey_db`;

-- ============================================
-- 1. 用户表
-- ============================================
CREATE TABLE `sys_user` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '自增主键，内部使用',
    `uuid` VARCHAR(32) NOT NULL UNIQUE COMMENT 'UUID，外部引用',
    `username` VARCHAR(50) NOT NULL UNIQUE COMMENT '登录账号',
    `password` VARCHAR(255) NOT NULL COMMENT 'BCrypt加密密码',
    `email` VARCHAR(100) UNIQUE COMMENT '邮箱，用于密码重置',
    `phone` VARCHAR(20) COMMENT '手机号',
    `name` VARCHAR(50) COMMENT '真实姓名',
    `avatar` VARCHAR(500) COMMENT '头像URL',
    `role` VARCHAR(20) NOT NULL DEFAULT 'user' COMMENT '角色: admin, user',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1-正常, 0-禁用',
    `email_verified` TINYINT DEFAULT 0 COMMENT '邮箱是否已验证: 0-未验证, 1-已验证',
    `last_login_time` DATETIME COMMENT '最后登录时间',
    `login_fail_count` INT DEFAULT 0 COMMENT '登录失败次数',
    `locked_until` DATETIME COMMENT '账户锁定截止时间',
    `password_reset_token` VARCHAR(100) COMMENT '密码重置令牌',
    `password_reset_expires` DATETIME COMMENT '重置令牌过期时间',
    `email_verify_token` VARCHAR(100) COMMENT '邮箱验证令牌',
    `email_verify_expires` DATETIME COMMENT '验证令牌过期时间',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',

    INDEX `idx_uuid` (`uuid`),
    INDEX `idx_username` (`username`),
    INDEX `idx_email` (`email`),
    INDEX `idx_status` (`status`),
    INDEX `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统用户表';

-- 插入默认管理员账号
-- 密码: admin123 (BCrypt加密)
INSERT INTO `sys_user` (`uuid`, `username`, `password`, `email`, `name`, `role`, `status`, `email_verified`)
VALUES (
    REPLACE(UUID(), '-', ''),
    'admin',
    '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.qVh.N9NJ.KQXCK',
    'admin@example.com',
    '管理员',
    'admin',
    1,
    1
);

-- ============================================
-- 2. 问卷模板表
-- ============================================
CREATE TABLE `survey_template` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '自增主键',
    `uuid` VARCHAR(32) NOT NULL UNIQUE COMMENT 'UUID，外部引用',
    `title` VARCHAR(200) NOT NULL COMMENT '问卷标题',
    `description` TEXT COMMENT '问卷描述',
    `form_id` VARCHAR(50) UNIQUE COMMENT '业务标识符',
    `schema_json` JSON NOT NULL COMMENT '表单JSON Schema',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态: 0-草稿, 1-已发布, 2-已归档',
    `version` INT NOT NULL DEFAULT 1 COMMENT '版本号',
    `start_time` DATETIME COMMENT '开始时间',
    `end_time` DATETIME COMMENT '结束时间',
    `max_submissions` INT DEFAULT 0 COMMENT '最大提交数，0表示不限制',
    `allow_edit` TINYINT DEFAULT 0 COMMENT '是否允许修改: 0-不允许, 1-允许',
    `allow_anonymous` TINYINT DEFAULT 0 COMMENT '是否允许匿名: 0-不允许, 1-允许',
    `created_by` BIGINT COMMENT '创建人ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `publish_time` DATETIME COMMENT '发布时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',

    INDEX `idx_uuid` (`uuid`),
    INDEX `idx_form_id` (`form_id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_created_by` (`created_by`),
    INDEX `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='问卷模板表';

-- ============================================
-- 3. 问卷答卷表
-- ============================================
CREATE TABLE `survey_answer` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '自增主键',
    `uuid` VARCHAR(32) NOT NULL UNIQUE COMMENT 'UUID，外部引用',
    `template_id` BIGINT NOT NULL COMMENT '问卷模板ID',
    `template_uuid` VARCHAR(32) NOT NULL COMMENT '问卷模板UUID',
    `user_id` BIGINT COMMENT '用户ID，匿名时为空',
    `user_uuid` VARCHAR(32) COMMENT '用户UUID',
    `answer_data` JSON NOT NULL COMMENT '答案数据',
    `total_score` DECIMAL(10, 3) DEFAULT 0 COMMENT '总分',
    `submit_ip` VARCHAR(50) COMMENT '提交IP',
    `user_agent` VARCHAR(500) COMMENT '浏览器UA',
    `duration_seconds` INT COMMENT '填写耗时(秒)',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',

    INDEX `idx_uuid` (`uuid`),
    INDEX `idx_template_id` (`template_id`),
    INDEX `idx_template_uuid` (`template_uuid`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_user_uuid` (`user_uuid`),
    INDEX `idx_create_time` (`create_time`),
    UNIQUE KEY `uk_template_user` (`template_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='问卷答卷表';

-- ============================================
-- 4. 操作日志表
-- ============================================
CREATE TABLE `sys_oper_log` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '自增主键',
    `trace_id` VARCHAR(32) NOT NULL COMMENT '追踪ID',
    `title` VARCHAR(100) DEFAULT '' COMMENT '操作标题',
    `business_type` VARCHAR(20) DEFAULT 'OTHER' COMMENT '业务类型: INSERT, UPDATE, DELETE, SELECT, EXPORT, IMPORT',
    `method` VARCHAR(200) DEFAULT '' COMMENT '方法名称',
    `request_method` VARCHAR(10) DEFAULT '' COMMENT '请求方式: GET, POST, PUT, DELETE',
    `oper_user_id` BIGINT COMMENT '操作用户ID',
    `oper_name` VARCHAR(50) COMMENT '操作用户名',
    `oper_url` VARCHAR(255) COMMENT '请求URL',
    `oper_ip` VARCHAR(50) COMMENT '操作IP',
    `oper_location` VARCHAR(100) COMMENT '操作地点',
    `oper_param` TEXT COMMENT '请求参数',
    `json_result` LONGTEXT COMMENT '返回结果',
    `status` TINYINT DEFAULT 0 COMMENT '状态: 0-成功, 1-失败',
    `error_msg` TEXT COMMENT '错误信息',
    `cost_time` BIGINT DEFAULT 0 COMMENT '耗时(毫秒)',
    `oper_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',

    INDEX `idx_trace_id` (`trace_id`),
    INDEX `idx_oper_user_id` (`oper_user_id`),
    INDEX `idx_oper_time` (`oper_time`),
    INDEX `idx_business_type` (`business_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志表';

-- ============================================
-- 5. 文件元数据表
-- ============================================
CREATE TABLE `sys_file` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '自增主键',
    `uuid` VARCHAR(32) NOT NULL UNIQUE COMMENT 'UUID',
    `original_name` VARCHAR(255) NOT NULL COMMENT '原始文件名',
    `stored_name` VARCHAR(255) NOT NULL COMMENT '存储文件名',
    `file_path` VARCHAR(500) NOT NULL COMMENT '文件路径',
    `file_url` VARCHAR(500) COMMENT '访问URL',
    `file_size` BIGINT NOT NULL COMMENT '文件大小(字节)',
    `file_type` VARCHAR(50) COMMENT 'MIME类型',
    `file_ext` VARCHAR(20) COMMENT '文件扩展名',
    `file_hash` VARCHAR(64) COMMENT 'SHA-256哈希，用于去重',
    `upload_user_id` BIGINT COMMENT '上传用户ID',
    `related_type` VARCHAR(50) COMMENT '关联类型: survey, avatar, temp',
    `related_id` VARCHAR(32) COMMENT '关联ID',
    `status` TINYINT DEFAULT 1 COMMENT '状态: 1-正常, 0-临时, -1-已删除',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',

    INDEX `idx_uuid` (`uuid`),
    INDEX `idx_file_hash` (`file_hash`),
    INDEX `idx_upload_user_id` (`upload_user_id`),
    INDEX `idx_related` (`related_type`, `related_id`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文件元数据表';

-- ============================================
-- 6. 邮件验证码表
-- ============================================
CREATE TABLE `sys_email_code` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `email` VARCHAR(100) NOT NULL COMMENT '邮箱地址',
    `code` VARCHAR(10) NOT NULL COMMENT '验证码',
    `type` VARCHAR(20) NOT NULL COMMENT '类型: REGISTER, RESET_PASSWORD, CHANGE_EMAIL',
    `used` TINYINT DEFAULT 0 COMMENT '是否已使用: 0-未使用, 1-已使用',
    `expire_time` DATETIME NOT NULL COMMENT '过期时间',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    INDEX `idx_email_type` (`email`, `type`),
    INDEX `idx_expire_time` (`expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='邮件验证码表';

-- ============================================
-- 7. 系统配置表
-- ============================================
CREATE TABLE `sys_config` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `config_key` VARCHAR(100) NOT NULL UNIQUE COMMENT '配置键',
    `config_value` TEXT COMMENT '配置值',
    `config_type` VARCHAR(50) DEFAULT 'STRING' COMMENT '类型: STRING, NUMBER, BOOLEAN, JSON',
    `description` VARCHAR(255) COMMENT '配置描述',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX `idx_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统配置表';

-- 插入默认配置
INSERT INTO `sys_config` (`config_key`, `config_value`, `config_type`, `description`) VALUES
('system.name', 'CC Survey', 'STRING', '系统名称'),
('system.logo', '', 'STRING', '系统Logo URL'),
('file.max_size', '20971520', 'NUMBER', '文件最大大小(字节)，默认20MB'),
('file.allowed_types', 'image/jpeg,image/png,image/gif,application/pdf,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet,application/vnd.openxmlformats-officedocument.wordprocessingml.document', 'STRING', '允许的文件类型'),
('login.max_fail_count', '5', 'NUMBER', '最大登录失败次数'),
('login.lock_duration', '1800', 'NUMBER', '账户锁定时长(秒)，默认30分钟'),
('jwt.expiration', '7200000', 'NUMBER', 'JWT过期时间(毫秒)，默认2小时'),
('jwt.refresh_expiration', '604800000', 'NUMBER', '刷新令牌过期时间(毫秒)，默认7天');
