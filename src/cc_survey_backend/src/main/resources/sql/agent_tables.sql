-- Agent 模块数据库表

-- AI 会话表
CREATE TABLE IF NOT EXISTS ai_conversation (
    id VARCHAR(32) NOT NULL PRIMARY KEY COMMENT '会话ID',
    user_id VARCHAR(32) NOT NULL COMMENT '用户ID',
    title VARCHAR(200) NOT NULL COMMENT '会话标题',
    agent_type VARCHAR(50) NOT NULL DEFAULT 'general' COMMENT 'Agent类型: survey, data, log, general',
    status INT NOT NULL DEFAULT 1 COMMENT '状态: 1-活跃, 0-归档',
    context JSON COMMENT '会话上下文',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user_id (user_id),
    INDEX idx_agent_type (agent_type),
    INDEX idx_updated_at (updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI会话表';

-- AI 消息表
CREATE TABLE IF NOT EXISTS ai_message (
    id VARCHAR(32) NOT NULL PRIMARY KEY COMMENT '消息ID',
    conversation_id VARCHAR(32) NOT NULL COMMENT '会话ID',
    role VARCHAR(20) NOT NULL COMMENT '角色: user, assistant, tool',
    content TEXT COMMENT '消息内容 (Markdown)',
    tool_calls JSON COMMENT 'Tool调用记录',
    tool_results JSON COMMENT 'Tool返回结果',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_conversation_id (conversation_id),
    INDEX idx_created_at (created_at),
    FOREIGN KEY (conversation_id) REFERENCES ai_conversation(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI消息表';
