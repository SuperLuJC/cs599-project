package com.ccsurvey.modules.agent.tool;

import com.ccsurvey.modules.agent.dto.ToolDefinition;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Agent 工具基类
 */
public abstract class AgentTool {

    protected final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 获取工具名称
     */
    public abstract String getName();

    /**
     * 获取工具描述
     */
    public abstract String getDescription();

    /**
     * 获取参数 Schema
     */
    public abstract Map<String, Object> getParametersSchema();

    /**
     * 执行工具
     */
    public abstract ToolResult execute(Map<String, Object> arguments, ToolContext context);

    /**
     * 转换为 ToolDefinition
     */
    public ToolDefinition toToolDefinition() {
        return ToolDefinition.of(getName(), getDescription(), getParametersSchema());
    }

    /**
     * 创建简单的参数 Schema
     */
    protected Map<String, Object> createSchema(List<Property> properties, List<String> required) {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");

        Map<String, Object> props = new HashMap<>();
        for (Property p : properties) {
            Map<String, Object> prop = new HashMap<>();
            prop.put("type", p.type);
            prop.put("description", p.description);
            if (p.enumValues != null) {
                prop.put("enum", p.enumValues);
            }
            props.put(p.name, prop);
        }
        schema.put("properties", props);
        schema.put("required", required);

        return schema;
    }

    public static class Property {
        String name;
        String type;
        String description;
        List<String> enumValues;

        public static Property of(String name, String type, String description) {
            Property p = new Property();
            p.name = name;
            p.type = type;
            p.description = description;
            return p;
        }

        public static Property ofEnum(String name, String description, List<String> enumValues) {
            Property p = new Property();
            p.name = name;
            p.type = "string";
            p.description = description;
            p.enumValues = enumValues;
            return p;
        }
    }
}
