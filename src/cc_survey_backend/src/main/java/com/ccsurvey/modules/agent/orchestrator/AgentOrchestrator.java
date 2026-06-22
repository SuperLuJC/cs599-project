package com.ccsurvey.modules.agent.orchestrator;

import com.ccsurvey.modules.agent.agent.*;
import com.ccsurvey.modules.agent.intent.Intent;
import com.ccsurvey.modules.agent.intent.IntentResult;
import com.ccsurvey.modules.agent.memory.AgentMemory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Agent 编排器
 * 根据意图选择合适的 Agent 并执行
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AgentOrchestrator {

    private final IntentRouter intentRouter;
    private final Map<String, Agent> agents;
    private final AgentMemory memory;

    /**
     * 执行 Agent 任务
     *
     * @param request Agent 请求
     * @return Agent 响应
     */
    public AgentResponse execute(AgentRequest request) {
        log.info("Orchestrating agent request: conversationId={}, userId={}",
                request.getConversationId(), request.getUserId());

        // 1. 意图识别
        IntentResult intentResult = intentRouter.route(request.getMessage(), null);
        request.setIntentResult(intentResult);

        // 2. 选择 Agent
        Agent selectedAgent = selectAgent(intentResult);

        if (selectedAgent == null) {
            log.error("No agent found for intent: {}", intentResult.getPrimaryIntent());
            return AgentResponse.error("No suitable agent available");
        }

        log.info("Selected agent: {} for intent: {}",
                selectedAgent.getName(), intentResult.getPrimaryIntent());

        // 3. 构建上下文
        AgentContext context = buildContext(request, intentResult);

        // 4. 执行 Agent
        AgentResponse response = selectedAgent.execute(request, context);

        // 5. 设置响应元数据
        response.setAgentType(selectedAgent.getType());

        return response;
    }

    /**
     * 根据意图选择 Agent
     */
    private Agent selectAgent(IntentResult intentResult) {
        Intent primaryIntent = intentResult.getPrimaryIntent();

        // 获取意图对应的 Agent 名称
        String agentName = intentRouter.getAgentForIntent(primaryIntent);

        // 查找 Agent
        Agent agent = agents.get(agentName);

        if (agent == null) {
            // 尝试使用 RouterAgent 作为后备
            agent = agents.get("routerAgent");
        }

        return agent;
    }

    /**
     * 构建执行上下文
     */
    private AgentContext buildContext(AgentRequest request, IntentResult intentResult) {
        AgentContext context = AgentContext.of(request.getConversationId(), request.getUserId());
        context.setAgentType(intentRouter.getAgentForIntent(intentResult.getPrimaryIntent()));
        context.setIntentResult(intentResult);

        // 加载会话上下文
        if (request.getConversationId() != null) {
            Map<String, Object> sessionContext = memory.getAllContext(request.getConversationId());
            context.setSessionContext(sessionContext);
        }

        return context;
    }

    /**
     * 获取所有可用的 Agent
     */
    public List<Agent> getAvailableAgents() {
        return List.copyOf(agents.values());
    }

    /**
     * 获取指定名称的 Agent
     */
    public Agent getAgent(String name) {
        return agents.get(name);
    }
}