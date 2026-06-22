package com.ccsurvey.modules.agent.dto;

import com.ccsurvey.modules.agent.intent.IntentResult;
import lombok.Data;

/**
 * Agent 请求
 */
@Data
public class AgentRequest {

    /**
     * 会话ID (可选，新会话时不传)
     */
    private String conversationId;

    /**
     * 用户ID
     */
    private String userId;

    /**
     * Agent 类型: survey, data, log, general
     */
    private String agentType = "general";

    /**
     * 用户消息
     */
    private String message;

    /**
     * 意图识别结果（内部使用）
     */
    private IntentResult intentResult;
}
