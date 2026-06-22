package com.ccsurvey.modules.agent.agent;

import com.ccsurvey.modules.agent.intent.Intent;
import com.ccsurvey.modules.agent.intent.IntentResult;
import dev.langchain4j.agent.tool.ToolSpecification;

import java.util.List;

/**
 * Agent 接口
 * 定义 Agent 的基本行为
 */
public interface Agent {

    /**
     * 获取 Agent 名称
     */
    String getName();

    /**
     * 获取 Agent 描述
     */
    String getDescription();

    /**
     * 获取 Agent 类型标识
     */
    String getType();

    /**
     * 执行 Agent 任务
     *
     * @param request 请求
     * @param context 上下文
     * @return 响应
     */
    AgentResponse execute(AgentRequest request, AgentContext context);

    /**
     * 获取 Agent 可用的工具列表
     */
    List<ToolSpecification> getTools();

    /**
     * 检查 Agent 是否能处理指定意图
     */
    boolean canHandle(Intent intent);

    /**
     * 获取 Agent 的系统提示词
     */
    String getSystemPrompt();
}