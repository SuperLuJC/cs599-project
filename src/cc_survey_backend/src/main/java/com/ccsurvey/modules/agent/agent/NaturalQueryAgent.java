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
 * 自然语言查询 Agent
 * 将用户的自然语言问题转换为数据库查询，返回真实数据
 */
@Slf4j
@Component
public class NaturalQueryAgent extends BaseAgent {

    private final AgentTool naturalQueryTool;

    public NaturalQueryAgent(ChatModel chatModel,
                              AgentMemory memory,
                              AgentProperties agentProperties,
                              AgentTool naturalQueryTool) {
        super(chatModel, memory, agentProperties);
        this.naturalQueryTool = naturalQueryTool;
    }

    @Override
    public String getName() {
        return "NaturalQueryAgent";
    }

    @Override
    public String getDescription() {
        return "自然语言查询助手，将用户问题转换为数据库查询，返回真实数据";
    }

    @Override
    public String getType() {
        return "natural_query";
    }

    @Override
    public String getSystemPrompt() {
        return SystemPrompts.getPrompt("natural_query");  // 动态获取，包含实时时间
    }

    @Override
    public List<ToolSpecification> getTools() {
        String enhancedDescription = naturalQueryTool.getDescription() + """


                ## 查询类型说明

                | query_type | 说明 | 主要参数 |
                |------------|------|----------|
                | survey_submission_stats | 问卷提交统计 | survey_title, time_start, time_end |
                | top_surveys | 提交量排行 | time_start, time_end, limit |
                | user_submission_stats | 用户提交统计 | user_name, time_start, time_end |
                | survey_avg_score | 问卷平均分 | survey_title |
                | login_stats | 登录统计 | time_start, time_end |
                | user_register_stats | 用户注册统计 | time_start, time_end |
                | surveys_no_submission | 无提交问卷 | 无 |
                | survey_detail | 问卷详情 | survey_title |

                ## 时间格式
                所有时间参数格式: yyyy-MM-dd HH:mm:ss
                例如: "2026-05-02 00:00:00"
                """;

        return List.of(
                ToolSpecification.builder()
                        .name("natural_query")
                        .description(enhancedDescription)
                        .build()
        );
    }

    @Override
    public boolean canHandle(Intent intent) {
        return intent == Intent.NATURAL_QUERY;
    }

    @Override
    protected String executeTool(String toolName, String arguments, AgentContext context) {
        if (!"natural_query".equals(toolName)) {
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

            ToolResult result = naturalQueryTool.execute(args, toolContext);

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
