package com.ccsurvey.modules.agent.intent;

import lombok.Getter;

/**
 * 意图枚举
 * 定义 Agent 可以识别的用户意图类型
 */
@Getter
public enum Intent {

    /**
     * 创建问卷
     */
    CREATE_SURVEY("create_survey", "创建问卷", "survey"),

    /**
     * 更新问卷
     */
    UPDATE_SURVEY("update_survey", "更新问卷", "survey"),

    /**
     * 数据分析
     */
    ANALYZE_DATA("analyze_data", "数据分析", "data"),

    /**
     * 自然语言查询
     */
    NATURAL_QUERY("natural_query", "自然语言查询", "natural_query"),

    /**
     * 日志查询
     */
    QUERY_LOGS("query_logs", "日志查询", "log"),

    /**
     * 通用对话
     */
    GENERAL_CHAT("general_chat", "通用对话", "general"),

    /**
     * 保存草稿
     */
    SAVE_DRAFT("save_draft", "保存草稿", "survey"),

    /**
     * 多 Agent 任务（需要多个 Agent 协作）
     */
    MULTI_AGENT_TASK("multi_agent_task", "复合任务", "general"),

    /**
     * 未知意图
     */
    UNKNOWN("unknown", "未知", "general");

    /**
     * 意图代码
     */
    private final String code;

    /**
     * 意图描述
     */
    private final String description;

    /**
     * 对应的 Agent 类型
     */
    private final String agentType;

    Intent(String code, String description, String agentType) {
        this.code = code;
        this.description = description;
        this.agentType = agentType;
    }

    /**
     * 根据代码获取意图
     */
    public static Intent fromCode(String code) {
        if (code == null) {
            return UNKNOWN;
        }

        for (Intent intent : values()) {
            if (intent.getCode().equalsIgnoreCase(code)) {
                return intent;
            }
        }

        return UNKNOWN;
    }

    /**
     * 根据 Agent 类型获取意图
     */
    public static Intent fromAgentType(String agentType) {
        if (agentType == null) {
            return GENERAL_CHAT;
        }

        switch (agentType.toLowerCase()) {
            case "survey":
                return CREATE_SURVEY;
            case "data":
                return ANALYZE_DATA;
            case "log":
                return QUERY_LOGS;
            case "natural_query":
                return NATURAL_QUERY;
            default:
                return GENERAL_CHAT;
        }
    }

    /**
     * 根据字符串名称获取意图（用于 LLM 返回的意图解析）
     */
    public static Intent fromString(String name) {
        if (name == null) {
            return UNKNOWN;
        }

        String upperName = name.toUpperCase().trim();
        for (Intent intent : values()) {
            if (intent.name().equals(upperName)) {
                return intent;
            }
            // 也匹配下划线格式
            if (intent.getCode().equalsIgnoreCase(name)) {
                return intent;
            }
        }

        return UNKNOWN;
    }
}