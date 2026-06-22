package com.ccsurvey.modules.agent.dto;

import lombok.Data;

/**
 * 会话信息
 */
@Data
public class ConversationInfo {

    /**
     * 会话ID
     */
    private String id;

    /**
     * 会话标题
     */
    private String title;

    /**
     * Agent 类型
     */
    private String agentType;

    /**
     * 创建时间
     */
    private String createdAt;
}
