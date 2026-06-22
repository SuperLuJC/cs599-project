package com.ccsurvey.modules.agent.orchestrator;

import com.ccsurvey.modules.agent.agent.*;
import com.ccsurvey.modules.agent.intent.Intent;
import com.ccsurvey.modules.agent.intent.IntentResult;
import com.ccsurvey.modules.agent.memory.AgentMemory;
import com.ccsurvey.modules.agent.protocol.AgentMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Agent 协调器
 * 处理多 Agent 协作场景
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AgentCoordinator {

    private final IntentRouter intentRouter;
    private final Map<String, Agent> agents;
    private final AgentMemory memory;
    private final ExecutorService executor = Executors.newFixedThreadPool(4);

    /**
     * 协调多个 Agent 执行任务
     *
     * @param request Agent 请求
     * @return 协调结果
     */
    public AgentResponse coordinate(AgentRequest request) {
        log.info("Coordinating multi-agent task: conversationId={}", request.getConversationId());

        IntentResult intentResult = request.getIntentResult();
        if (intentResult == null) {
            intentResult = intentRouter.route(request.getMessage(), null);
        }

        // 获取需要执行的 Agent 列表
        List<Intent> intents = collectIntents(intentResult);
        List<String> agentNames = intentRouter.getAgentsForIntents(intents);

        if (agentNames.isEmpty()) {
            return AgentResponse.error("No agents available for coordination");
        }

        // 单 Agent 场景
        if (agentNames.size() == 1) {
            Agent agent = agents.get(agentNames.get(0));
            if (agent != null) {
                AgentContext context = buildContext(request, intentResult);
                return agent.execute(request, context);
            }
        }

        // 多 Agent 场景：判断执行策略
        if (shouldExecuteInParallel(intents)) {
            return executeParallel(request, intentResult, agentNames);
        } else {
            return executeSequential(request, intentResult, agentNames);
        }
    }

    /**
     * 收集所有意图
     */
    private List<Intent> collectIntents(IntentResult intentResult) {
        List<Intent> intents = new ArrayList<>();
        intents.add(intentResult.getPrimaryIntent());

        if (intentResult.getSecondaryIntents() != null) {
            intents.addAll(intentResult.getSecondaryIntents());
        }

        return intents;
    }

    /**
     * 判断是否应该并行执行
     */
    private boolean shouldExecuteInParallel(List<Intent> intents) {
        // 如果意图之间没有依赖关系，可以并行执行
        // 例如：创建问卷 + 数据分析 是独立的
        // 但：创建问卷 + 更新问卷 是有依赖的

        Set<String> agentTypes = new HashSet<>();
        for (Intent intent : intents) {
            agentTypes.add(intent.getAgentType());
        }

        // 如果都是不同类型的 Agent，可以并行
        return agentTypes.size() == intents.size();
    }

    /**
     * 并行执行多个 Agent
     */
    private AgentResponse executeParallel(AgentRequest request,
                                            IntentResult intentResult,
                                            List<String> agentNames) {
        log.info("Executing {} agents in parallel", agentNames.size());

        List<CompletableFuture<AgentResponse>> futures = new ArrayList<>();

        for (String agentName : agentNames) {
            Agent agent = agents.get(agentName);
            if (agent == null) continue;

            CompletableFuture<AgentResponse> future = CompletableFuture.supplyAsync(() -> {
                AgentContext context = buildContext(request, intentResult);
                return agent.execute(request, context);
            }, executor);

            futures.add(future);
        }

        // 等待所有 Agent 完成
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // 收集结果
        List<AgentResponse> responses = new ArrayList<>();
        for (CompletableFuture<AgentResponse> future : futures) {
            try {
                responses.add(future.get());
            } catch (Exception e) {
                log.error("Failed to get agent response: {}", e.getMessage());
            }
        }

        // 合并结果
        return mergeResponses(responses, request.getConversationId());
    }

    /**
     * 串行执行多个 Agent
     */
    private AgentResponse executeSequential(AgentRequest request,
                                              IntentResult intentResult,
                                              List<String> agentNames) {
        log.info("Executing {} agents sequentially", agentNames.size());

        List<AgentResponse> responses = new ArrayList<>();
        Map<String, Object> accumulatedContext = new HashMap<>();

        for (String agentName : agentNames) {
            Agent agent = agents.get(agentName);
            if (agent == null) continue;

            AgentContext context = buildContext(request, intentResult);
            context.getSessionContext().putAll(accumulatedContext);

            AgentResponse response = agent.execute(request, context);
            responses.add(response);

            // 累积上下文
            if (response.getMetadata() != null) {
                accumulatedContext.putAll(response.getMetadata());
            }

            // 如果某个 Agent 失败，停止执行
            if (!response.isSuccess()) {
                log.warn("Agent {} failed, stopping sequential execution", agentName);
                break;
            }
        }

        return mergeResponses(responses, request.getConversationId());
    }

    /**
     * 合并多个 Agent 的响应
     */
    private AgentResponse mergeResponses(List<AgentResponse> responses, String conversationId) {
        if (responses.isEmpty()) {
            return AgentResponse.error("No agent responses to merge");
        }

        if (responses.size() == 1) {
            return responses.get(0);
        }

        // 合并内容
        StringBuilder mergedContent = new StringBuilder();
        boolean allSuccess = true;

        for (int i = 0; i < responses.size(); i++) {
            AgentResponse response = responses.get(i);

            if (!response.isSuccess()) {
                allSuccess = false;
            }

            if (i > 0) {
                mergedContent.append("\n\n---\n\n");
            }

            mergedContent.append(response.getContent());
        }

        AgentResponse merged = new AgentResponse();
        merged.setSuccess(allSuccess);
        merged.setConversationId(conversationId);
        merged.setContent(mergedContent.toString());

        return merged;
    }

    /**
     * 构建执行上下文
     */
    private AgentContext buildContext(AgentRequest request, IntentResult intentResult) {
        AgentContext context = AgentContext.of(request.getConversationId(), request.getUserId());
        context.setIntentResult(intentResult);

        if (request.getConversationId() != null) {
            Map<String, Object> sessionContext = memory.getAllContext(request.getConversationId());
            context.setSessionContext(sessionContext);
        }

        return context;
    }

    /**
     * 发送 Agent 间消息
     */
    public void sendMessage(AgentMessage message) {
        Agent targetAgent = agents.get(message.getTargetAgent());
        if (targetAgent != null) {
            log.debug("Sending message from {} to {}: {}",
                    message.getSourceAgent(), message.getTargetAgent(), message.getType());
        }
    }
}