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
 * 日志分析 Agent
 * 负责系统操作日志的查询和分析
 */
@Slf4j
@Component
public class LogAnalysisAgent extends BaseAgent {

    private final AgentTool logAnalysisTool;

    public LogAnalysisAgent(ChatModel chatModel,
                             AgentMemory memory,
                             AgentProperties agentProperties,
                             AgentTool logAnalysisTool) {
        super(chatModel, memory, agentProperties);
        this.logAnalysisTool = logAnalysisTool;
    }

    @Override
    public String getName() {
        return "LogAnalysisAgent";
    }

    @Override
    public String getDescription() {
        return "日志分析助手，帮助用户查询系统操作日志";
    }

    @Override
    public String getType() {
        return "log";
    }

    @Override
    public String getSystemPrompt() {
        return SystemPrompts.LOG_ASSISTANT;
    }

    @Override
    public List<ToolSpecification> getTools() {
        String enhancedDescription = logAnalysisTool.getDescription();

        return List.of(
                ToolSpecification.builder()
                        .name("analyze_logs")
                        .description(enhancedDescription)
                        .build()
        );
    }

    @Override
    public boolean canHandle(Intent intent) {
        return intent == Intent.QUERY_LOGS;
    }

    @Override
    protected String executeTool(String toolName, String arguments, AgentContext context) {
        if (!"analyze_logs".equals(toolName)) {
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

            ToolResult result = logAnalysisTool.execute(args, toolContext);

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