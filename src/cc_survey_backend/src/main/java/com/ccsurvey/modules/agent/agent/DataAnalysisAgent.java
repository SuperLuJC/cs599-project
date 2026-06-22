package com.ccsurvey.modules.agent.agent;

import com.ccsurvey.modules.agent.config.AgentProperties;
import com.ccsurvey.modules.agent.intent.Intent;
import com.ccsurvey.modules.agent.memory.AgentMemory;
import com.ccsurvey.modules.agent.prompt.SystemPrompts;
import com.ccsurvey.modules.agent.tool.AgentTool;
import com.ccsurvey.modules.agent.tool.ToolContext;
import com.ccsurvey.modules.agent.tool.ToolResult;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.model.chat.ChatModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 数据分析 Agent
 * 负责问卷提交数据的分析和统计
 */
@Slf4j
@Component
public class DataAnalysisAgent extends BaseAgent {

    private final AgentTool dataAnalysisTool;

    public DataAnalysisAgent(ChatModel chatModel,
                              AgentMemory memory,
                              AgentProperties agentProperties,
                              AgentTool dataAnalysisTool) {
        super(chatModel, memory, agentProperties);
        this.dataAnalysisTool = dataAnalysisTool;
    }

    @Override
    public String getName() {
        return "DataAnalysisAgent";
    }

    @Override
    public String getDescription() {
        return "数据分析助手，帮助用户分析问卷提交数据";
    }

    @Override
    public String getType() {
        return "data";
    }

    @Override
    public String getSystemPrompt() {
        return SystemPrompts.DATA_ASSISTANT;
    }

    @Override
    public List<ToolSpecification> getTools() {
        // 使用工具类的详细描述
        String enhancedDescription = dataAnalysisTool.getDescription();

        return List.of(
                ToolSpecification.builder()
                        .name("analyze_data")
                        .description(enhancedDescription)
                        .build()
        );
    }

    @Override
    public boolean canHandle(Intent intent) {
        return intent == Intent.ANALYZE_DATA;
    }

    @Override
    protected String executeTool(String toolName, String arguments, AgentContext context) {
        if (!"analyze_data".equals(toolName)) {
            return "Unknown tool: " + toolName;
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> args = com.alibaba.fastjson2.JSON.parseObject(arguments, Map.class);

            ToolContext toolContext = new ToolContext();
            toolContext.setUserId(context.getUserId());
            toolContext.setConversationId(context.getConversationId());
            toolContext.setAgentType(getType());
            toolContext.setSessionContext(context.getSessionContext());

            ToolResult result = dataAnalysisTool.execute(args, toolContext);

            if (result.isSuccess()) {
                return com.alibaba.fastjson2.JSON.toJSONString(result.getData());
            } else {
                return "Error: " + result.getError();
            }

        } catch (Exception e) {
            log.error("Tool execution failed: {}", e.getMessage(), e);
            return "Error: " + e.getMessage();
        }
    }
}