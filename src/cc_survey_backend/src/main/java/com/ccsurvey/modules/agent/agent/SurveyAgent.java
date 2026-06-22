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
 * 问卷构建 Agent
 * 负责问卷的创建、更新等操作
 */
@Slf4j
@Component
public class SurveyAgent extends BaseAgent {

    private final AgentTool surveyBuildTool;

    public SurveyAgent(ChatModel chatModel,
                       AgentMemory memory,
                       AgentProperties agentProperties,
                       AgentTool surveyBuildTool) {
        super(chatModel, memory, agentProperties);
        this.surveyBuildTool = surveyBuildTool;
    }

    @Override
    public String getName() {
        return "SurveyAgent";
    }

    @Override
    public String getDescription() {
        return "问卷构建助手，帮助用户创建、修改问卷";
    }

    @Override
    public String getType() {
        return "survey";
    }

    @Override
    public String getSystemPrompt() {
        return SystemPrompts.getPrompt("survey");  // 动态获取，包含实时时间
    }

    @Override
    public List<ToolSpecification> getTools() {
        // 构建完整的工具定义，包含参数说明
        String enhancedDescription = surveyBuildTool.getDescription() + """

                参数说明：
                - action (必填): 操作类型，可选值: preview, confirm, update
                - title: 问卷标题 (preview 时必填)
                - fields: 字段数组 (preview 时必填)，每个字段包含:
                  - name: 字段名称
                  - label: 显示标签
                  - type: 字段类型 (input/radio/checkbox/select/date/upload)
                  - options: 选项数组 (radio/checkbox/select 需要)，格式: [{"label": "选项文本", "value": "选项值"}]
                  - required: 是否必填
                - preview_id: 预览ID (confirm 时必填，从 preview 结果获取)
                - show_score: 是否展示评分 (默认 false)
                - max_submissions: 最大提交份数 (默认 0，表示不限制)
                - allow_edit: 是否允许用户修改提交 (默认 false)
                - allow_anonymous: 是否允许匿名提交 (默认 false)
                - start_time: 问卷开始时间 (格式: yyyy-MM-dd HH:mm:ss)
                - end_time: 问卷结束时间 (格式: yyyy-MM-dd HH:mm:ss)
                """;

        return List.of(
                ToolSpecification.builder()
                        .name("build_survey")
                        .description(enhancedDescription)
                        .build()
        );
    }

    @Override
    public boolean canHandle(Intent intent) {
        return intent == Intent.CREATE_SURVEY ||
               intent == Intent.UPDATE_SURVEY ||
               intent == Intent.SAVE_DRAFT;
    }

    @Override
    protected String executeTool(String toolName, String arguments, AgentContext context) {
        if (!"build_survey".equals(toolName)) {
            return "Unknown tool: " + toolName;
        }

        try {
            // 解析参数
            @SuppressWarnings("unchecked")
            Map<String, Object> args = com.alibaba.fastjson2.JSON.parseObject(arguments, Map.class);

            // 构建工具上下文
            ToolContext toolContext = new ToolContext();
            toolContext.setUserId(context.getUserId());
            toolContext.setConversationId(context.getConversationId());
            toolContext.setAgentType(getType());
            toolContext.setSessionContext(context.getSessionContext());

            // 执行工具
            ToolResult result = surveyBuildTool.execute(args, toolContext);

            // 更新上下文
            if (toolContext.getSessionContext() != null) {
                toolContext.getSessionContext().forEach((k, v) -> context.set(k, v));
            }

            // 返回结果
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