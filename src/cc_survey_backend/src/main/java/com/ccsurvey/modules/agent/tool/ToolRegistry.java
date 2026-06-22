package com.ccsurvey.modules.agent.tool;

import com.ccsurvey.modules.agent.dto.ToolDefinition;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 工具注册中心
 */
@Component
@RequiredArgsConstructor
public class ToolRegistry {

    private final List<AgentTool> tools;
    private final Map<String, AgentTool> toolMap = new HashMap<>();

    /**
     * 初始化工具映射
     */
    @jakarta.annotation.PostConstruct
    public void init() {
        for (AgentTool tool : tools) {
            toolMap.put(tool.getName(), tool);
        }
    }

    /**
     * 获取工具
     */
    public AgentTool getTool(String name) {
        return toolMap.get(name);
    }

    /**
     * 获取所有工具定义
     */
    public List<ToolDefinition> getAllToolDefinitions() {
        return tools.stream()
                .map(AgentTool::toToolDefinition)
                .toList();
    }

    /**
     * 检查工具是否存在
     */
    public boolean hasTool(String name) {
        return toolMap.containsKey(name);
    }
}
