package com.ccsurvey.modules.agent.agent;

import com.ccsurvey.modules.agent.config.AgentProperties;
import com.ccsurvey.modules.agent.intent.Intent;
import com.ccsurvey.modules.agent.memory.AgentMemory;
import com.ccsurvey.modules.agent.prompt.SystemPrompts;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.model.chat.ChatModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 路由 Agent
 * 通用对话助手，处理无法分类到特定 Agent 的请求
 */
@Slf4j
@Component
public class RouterAgent extends BaseAgent {

    public RouterAgent(ChatModel chatModel,
                       AgentMemory memory,
                       AgentProperties agentProperties) {
        super(chatModel, memory, agentProperties);
    }

    @Override
    public String getName() {
        return "RouterAgent";
    }

    @Override
    public String getDescription() {
        return "通用助手，处理一般性对话和无法分类的请求";
    }

    @Override
    public String getType() {
        return "general";
    }

    @Override
    public String getSystemPrompt() {
        return SystemPrompts.GENERAL_ASSISTANT;
    }

    @Override
    public List<ToolSpecification> getTools() {
        // 通用助手不使用工具
        return List.of();
    }

    @Override
    public boolean canHandle(Intent intent) {
        // 通用助手可以处理所有意图（作为后备）
        return true;
    }
}