package com.ccsurvey.modules.agent.dto;

import lombok.Data;

import java.util.Map;

/**
 * 工具定义
 */
@Data
public class ToolDefinition {

    /**
     * 工具名称
     */
    private String name;

    /**
     * 工具描述
     */
    private String description;

    /**
     * 参数 JSON Schema
     */
    private Map<String, Object> parameters;

    public static ToolDefinition of(String name, String description, Map<String, Object> parameters) {
        ToolDefinition tool = new ToolDefinition();
        tool.setName(name);
        tool.setDescription(description);
        tool.setParameters(parameters);
        return tool;
    }
}
