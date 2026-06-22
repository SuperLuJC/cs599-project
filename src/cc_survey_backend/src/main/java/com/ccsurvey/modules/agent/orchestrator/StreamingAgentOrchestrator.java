package com.ccsurvey.modules.agent.orchestrator;

import com.ccsurvey.modules.agent.agent.Agent;
import com.ccsurvey.modules.agent.agent.AgentContext;
import com.ccsurvey.modules.agent.agent.AgentRequest;
import com.ccsurvey.modules.agent.dto.StreamEvent;
import com.ccsurvey.modules.agent.entity.AiConversation;
import com.ccsurvey.modules.agent.intent.Intent;
import com.ccsurvey.modules.agent.intent.IntentResult;
import com.ccsurvey.modules.agent.memory.AgentMemory;
import com.ccsurvey.modules.agent.service.AgentConversationService;
import com.ccsurvey.modules.agent.tool.AgentTool;
import com.ccsurvey.modules.agent.tool.ToolContext;
import com.ccsurvey.modules.agent.tool.ToolResult;
import com.alibaba.fastjson2.JSON;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.*;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 流式 Agent 编排器
 * 基于 LangChain4j 实现流式对话，支持工具调用
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StreamingAgentOrchestrator {

    private final IntentRouter intentRouter;
    private final Map<String, Agent> agents;
    private final AgentMemory memory;
    private final ChatModel chatModel;
    private final StreamingChatModel streamingChatModel;
    private final AgentConversationService conversationService;
    private final List<AgentTool> agentTools;  // 注入所有工具

    private final ExecutorService executor = Executors.newCachedThreadPool();

    /**
     * 流式执行 Agent 任务
     */
    public void executeStream(AgentRequest request, SseEmitter emitter) {
        executor.execute(() -> {
            try {
                processStreamRequest(request, emitter);
            } catch (Exception e) {
                log.error("Stream execution error", e);
                try {
                    emitter.send(SseEmitter.event().data(StreamEvent.error(e.getMessage())));
                    emitter.complete();
                } catch (IOException ignored) {
                }
            }
        });
    }

    /**
     * 处理流式请求
     */
    private void processStreamRequest(AgentRequest request, SseEmitter emitter) throws IOException {
        boolean isNewConversation = request.getConversationId() == null || request.getConversationId().isEmpty();

        // 1. 获取或创建会话
        AiConversation conversation = conversationService.getOrCreateConversation(
                request.getConversationId(),
                request.getUserId(),
                request.getAgentType() != null ? request.getAgentType() : "general"
        );

        // 发送会话创建事件
        if (isNewConversation) {
            emitter.send(SseEmitter.event().data(StreamEvent.conversationCreated(conversation.getId())));
        }

        // 2. 加载会话上下文（用于意图识别）
        Map<String, Object> sessionContext = new HashMap<>();
        if (conversation.getId() != null) {
            Map<String, Object> loadedContext = memory.getAllContext(conversation.getId());
            if (loadedContext != null && !loadedContext.isEmpty()) {
                sessionContext.putAll(loadedContext);
            }
        }

        // 3. 意图识别（传递上下文以便检查是否有待确认的预览）
        IntentResult intentResult = intentRouter.route(request.getMessage(), sessionContext);
        request.setIntentResult(intentResult);

        log.info("Intent recognized: {} with confidence {}",
                intentResult.getPrimaryIntent(), intentResult.getConfidence());

        // 4. 选择 Agent
        Agent selectedAgent = selectAgent(intentResult, request.getAgentType());

        if (selectedAgent == null) {
            log.error("No agent found for intent: {}", intentResult.getPrimaryIntent());
            emitter.send(SseEmitter.event().data(StreamEvent.error("No suitable agent available")));
            emitter.complete();
            return;
        }

        log.info("Selected agent: {} for intent: {}", selectedAgent.getName(), intentResult.getPrimaryIntent());

        // 5. 构建上下文
        AgentContext context = buildContext(request, intentResult, conversation.getId());
        if (sessionContext != null) {
            context.setSessionContext(sessionContext);
        }

        // 6. 执行流式对话
        executeStreamingChat(selectedAgent, context, emitter, conversation.getId(),
                isNewConversation, request.getMessage());
    }

    /**
     * 根据意图选择 Agent
     */
    private Agent selectAgent(IntentResult intentResult, String preferredAgentType) {
        // 如果用户指定了 Agent 类型，优先使用
        if (preferredAgentType != null && !preferredAgentType.equals("general")) {
            for (Agent agent : agents.values()) {
                if (agent.getType().equals(preferredAgentType)) {
                    return agent;
                }
            }
        }

        // 根据意图选择
        Intent primaryIntent = intentResult.getPrimaryIntent();
        String agentName = intentRouter.getAgentForIntent(primaryIntent);
        Agent agent = agents.get(agentName);

        if (agent == null) {
            // 使用 RouterAgent 作为后备
            agent = agents.get("routerAgent");
        }

        return agent;
    }

    /**
     * 构建执行上下文
     */
    private AgentContext buildContext(AgentRequest request, IntentResult intentResult, String conversationId) {
        AgentContext context = AgentContext.of(conversationId, request.getUserId(), request.getMessage());
        context.setAgentType(intentRouter.getAgentForIntent(intentResult.getPrimaryIntent()));
        context.setIntentResult(intentResult);

        // 加载会话上下文
        if (conversationId != null) {
            Map<String, Object> loadedContext = memory.getAllContext(conversationId);
            if (loadedContext != null && !loadedContext.isEmpty()) {
                context.setSessionContext(new HashMap<>(loadedContext));
            }
        }

        return context;
    }

    /**
     * 执行流式对话
     * 对于有工具的 Agent，先执行工具调用循环，再流式输出最终响应
     */
    private void executeStreamingChat(Agent agent, AgentContext context, SseEmitter emitter,
                                       String conversationId, boolean isNewConversation, String userMessage) {
        try {
            // 1. 加载历史记忆（不包含当前用户消息）
            List<ChatMessage> history = loadHistory(conversationId);

            // 2. 构建消息列表
            List<ChatMessage> messages = buildMessages(history, agent.getSystemPrompt(), context);

            // 3. 添加当前用户消息
            messages.add(UserMessage.from(context.getUserMessage()));

            // 4. 检查 Agent 是否有工具
            List<ToolSpecification> tools = agent.getTools();
            boolean hasTools = tools != null && !tools.isEmpty();

            String finalResponse = null;

            if (hasTools) {
                // 有工具：执行工具调用循环，返回最终响应
                log.debug("Agent {} has {} tools, executing tool loop first", agent.getName(), tools.size());
                finalResponse = executeToolLoopAndGetFinalResponse(agent, messages, tools, context, emitter,
                        conversationId, isNewConversation, userMessage);
            }

            if (finalResponse != null) {
                // 工具调用已完成，最终响应已在工具循环中生成
                // 直接流式输出
                try {
                    for (char c : finalResponse.toCharArray()) {
                        emitter.send(SseEmitter.event().data(StreamEvent.content(String.valueOf(c))));
                    }

                    // 保存用户消息和 AI 响应到记忆
                    memory.addMessage(conversationId, UserMessage.from(userMessage));
                    memory.addMessage(conversationId, AiMessage.from(finalResponse));

                    // 如果是新会话，生成标题
                    if (isNewConversation) {
                        String title = conversationService.generateTitle(userMessage);
                        conversationService.updateTitle(conversationId, title);
                        emitter.send(SseEmitter.event().data(StreamEvent.title(title)));
                    }

                    emitter.send(SseEmitter.event().data(StreamEvent.done()));
                    emitter.complete();

                    log.debug("Stream completed for conversation: {}, content length: {}",
                            conversationId, finalResponse.length());
                } catch (IOException e) {
                    log.error("Failed to send response", e);
                }
            } else {
                // 没有工具或工具调用未产生最终响应，使用流式输出
                streamFinalResponse(messages, emitter, conversationId, isNewConversation, userMessage);
            }

        } catch (Exception e) {
            log.error("Failed to execute streaming chat for conversation: {}", conversationId, e);

            try {
                String errorMsg = e.getMessage();
                if (errorMsg != null && errorMsg.contains("EngineInternalError")) {
                    errorMsg = "AI 服务暂时繁忙，请稍后重试";
                }
                emitter.send(SseEmitter.event().data(StreamEvent.error(errorMsg)));
                emitter.complete();
            } catch (IOException ignored) {
            }
        }
    }

    /**
     * 执行工具调用循环并获取最终响应
     * 使用 ChatModel 执行，直到 LLM 不再请求工具调用，返回最终文本响应
     */
    private String executeToolLoopAndGetFinalResponse(Agent agent, List<ChatMessage> messages,
                                                        List<ToolSpecification> tools, AgentContext context,
                                                        SseEmitter emitter, String conversationId,
                                                        boolean isNewConversation, String userMessage) {
        int maxIterations = 5;
        int iteration = 0;
        String finalResponse = null;

        while (iteration < maxIterations) {
            iteration++;
            log.debug("Tool loop iteration {}/{}", iteration, maxIterations);

            // 构建 ChatRequest，包含工具定义
            ChatRequest chatRequest = ChatRequest.builder()
                    .messages(messages)
                    .toolSpecifications(tools)
                    .build();

            // 调用 LLM
            ChatResponse response = chatModel.chat(chatRequest);
            AiMessage aiMessage = response.aiMessage();

            // 检查是否有工具调用
            if (aiMessage.hasToolExecutionRequests()) {
                log.info("LLM requested {} tool executions", aiMessage.toolExecutionRequests().size());

                // 添加 AI 消息到历史
                messages.add(aiMessage);

                // 执行每个工具调用
                for (ToolExecutionRequest toolRequest : aiMessage.toolExecutionRequests()) {
                    String toolName = toolRequest.name();
                    String toolArgs = toolRequest.arguments();

                    log.info("Executing tool: {} with args: {}", toolName, toolArgs);

                    try {
                        // 执行工具
                        String result = executeToolForAgent(agent, toolName, toolArgs, context);

                        // 添加工具结果到消息
                        messages.add(ToolExecutionResultMessage.from(
                                toolRequest,
                                result
                        ));

                        log.debug("Tool {} executed successfully, result length: {}", toolName, result.length());

                    } catch (Exception e) {
                        log.error("Tool execution failed: {}", e.getMessage(), e);

                        // 添加错误结果
                        messages.add(ToolExecutionResultMessage.from(
                                toolRequest,
                                "Error: " + e.getMessage()
                        ));
                    }
                }

                // 继续循环，让 LLM 处理工具结果
                continue;
            }

            // 没有工具调用，获取最终响应
            finalResponse = aiMessage.text();
            log.debug("Tool loop completed after {} iterations, response length: {}", iteration,
                    finalResponse != null ? finalResponse.length() : 0);
            break;
        }

        if (iteration >= maxIterations) {
            log.warn("Reached max tool loop iterations: {}", maxIterations);
        }

        return finalResponse;
    }

    /**
     * 为 Agent 执行工具
     */
    private String executeToolForAgent(Agent agent, String toolName, String arguments, AgentContext context) {
        try {
            // 解析参数
            @SuppressWarnings("unchecked")
            Map<String, Object> args = JSON.parseObject(arguments, Map.class);

            // 构建工具上下文
            ToolContext toolContext = new ToolContext();
            toolContext.setUserId(context.getUserId());
            toolContext.setConversationId(context.getConversationId());
            toolContext.setAgentType(agent.getType());
            toolContext.setSessionContext(context.getSessionContext());

            // 获取工具实例
            var tool = getToolForAgent(agent, toolName);
            if (tool == null) {
                return "Error: Tool '" + toolName + "' not found";
            }

            // 执行工具
            ToolResult result = tool.execute(args, toolContext);

            // 更新上下文
            if (toolContext.getSessionContext() != null) {
                toolContext.getSessionContext().forEach((k, v) -> context.set(k, v));
                // 同步到 memory
                if (context.getConversationId() != null) {
                    memory.setContext(context.getConversationId(), "pendingPreviews",
                            toolContext.getSessionContext().get("pendingPreviews"));
                }
            }

            // 返回结果
            if (result.isSuccess()) {
                try {
                    // 如果需要确认，返回确认消息而不是原始数据
                    if (result.isRequiresConfirmation()) {
                        Map<String, Object> responseMap = new HashMap<>();
                        responseMap.put("status", "needs_confirmation");
                        responseMap.put("message", result.getConfirmationMessage());
                        responseMap.put("data", result.getData());
                        return JSON.toJSONString(responseMap);
                    }
                    return JSON.toJSONString(result.getData());
                } catch (Exception jsonException) {
                    log.error("JSON serialization failed: {}", jsonException.getMessage(), jsonException);
                    return "Error: JSON序列化失败 - " + jsonException.getClass().getSimpleName();
                }
            } else {
                return "Error: " + result.getError();
            }

        } catch (Exception e) {
            log.error("Tool execution failed: {}", e.getMessage(), e);
            return "Error: " + e.getClass().getSimpleName() + " - " + e.getMessage();
        }
    }

    /**
     * 获取 Agent 的工具实例
     */
    private AgentTool getToolForAgent(Agent agent, String toolName) {
        // 从注入的工具列表中查找
        for (AgentTool tool : agentTools) {
            if (tool.getName().equals(toolName)) {
                return tool;
            }
        }
        return null;
    }

    /**
     * 流式输出最终响应（用于没有工具调用的 Agent）
     */
    private void streamFinalResponse(List<ChatMessage> messages, SseEmitter emitter,
                                      String conversationId, boolean isNewConversation, String userMessage) {
        StringBuilder fullResponse = new StringBuilder();

        StreamingChatResponseHandler handler = new StreamingChatResponseHandler() {
            @Override
            public void onPartialResponse(String partialResponse) {
                fullResponse.append(partialResponse);
                try {
                    emitter.send(SseEmitter.event().data(StreamEvent.content(partialResponse)));
                } catch (IOException e) {
                    log.error("Failed to send partial response", e);
                }
            }

            @Override
            public void onCompleteResponse(ChatResponse completeResponse) {
                try {
                    // 保存用户消息和 AI 响应到记忆
                    memory.addMessage(conversationId, UserMessage.from(userMessage));
                    memory.addMessage(conversationId, AiMessage.from(fullResponse.toString()));

                    log.debug("Saved messages to memory for conversation: {}", conversationId);

                    // 如果是新会话，生成标题
                    if (isNewConversation) {
                        String title = conversationService.generateTitle(userMessage);
                        conversationService.updateTitle(conversationId, title);
                        emitter.send(SseEmitter.event().data(StreamEvent.title(title)));
                    }

                    // 发送完成事件
                    emitter.send(SseEmitter.event().data(StreamEvent.done()));
                    emitter.complete();

                    log.debug("Stream completed for conversation: {}, content length: {}",
                            conversationId, fullResponse.length());
                } catch (Exception e) {
                    log.error("Failed to complete stream", e);
                    try {
                        emitter.send(SseEmitter.event().data(StreamEvent.error(e.getMessage())));
                        emitter.complete();
                    } catch (IOException ignored) {
                    }
                }
            }

            @Override
            public void onError(Throwable error) {
                log.error("Stream error for conversation: {}", conversationId, error);

                // 保存用户消息和部分内容到记忆
                try {
                    memory.addMessage(conversationId, UserMessage.from(userMessage));
                    if (fullResponse.length() > 0) {
                        memory.addMessage(conversationId, AiMessage.from(fullResponse.toString() + " [响应中断]"));
                    }
                } catch (Exception e) {
                    log.error("Failed to save partial response", e);
                }

                try {
                    emitter.send(SseEmitter.event().data(StreamEvent.error(error.getMessage())));
                    emitter.complete();
                } catch (IOException ignored) {
                }
            }
        };

        // 执行流式调用
        streamingChatModel.chat(messages, handler);
    }

    /**
     * 加载历史记忆
     */
    private List<ChatMessage> loadHistory(String conversationId) {
        if (conversationId == null) {
            return new ArrayList<>();
        }

        List<ChatMessage> history = memory.getMessages(conversationId);
        log.debug("Loaded {} messages from memory", history.size());
        return history;
    }

    /**
     * 构建消息列表
     */
    private List<ChatMessage> buildMessages(List<ChatMessage> history, String systemPrompt, AgentContext context) {
        List<ChatMessage> messages = new ArrayList<>();

        // 构建增强的系统提示（包含上下文信息）
        String enhancedPrompt = systemPrompt;
        if (context.getSessionContext() != null) {
            Object pendingPreviews = context.getSessionContext().get("pendingPreviews");
            if (pendingPreviews instanceof Map && !((Map<?, ?>) pendingPreviews).isEmpty()) {
                // 有待确认的预览，添加上下文信息
                @SuppressWarnings("unchecked")
                Map<String, Object> previews = (Map<String, Object>) pendingPreviews;
                StringBuilder contextInfo = new StringBuilder("\n\n## 当前上下文状态\n\n");
                contextInfo.append("有待确认的问卷预览，等待用户确认保存：\n");
                for (Map.Entry<String, Object> entry : previews.entrySet()) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> preview = (Map<String, Object>) entry.getValue();
                    contextInfo.append("- preview_id: ").append(entry.getKey()).append("\n");
                    contextInfo.append("  标题: ").append(preview.get("title")).append("\n");
                    contextInfo.append("  字段数: ").append(preview.get("fields") instanceof List ? ((List<?>) preview.get("fields")).size() : "?").append("\n");
                }
                contextInfo.append("\n**如果用户确认保存，请立即调用 build_survey 工具执行 confirm 操作，使用上述 preview_id。**\n");
                enhancedPrompt = systemPrompt + contextInfo.toString();
                log.debug("Enhanced system prompt with {} pending previews", previews.size());
            }
        }

        // 添加系统提示
        if (enhancedPrompt != null && !enhancedPrompt.isEmpty()) {
            messages.add(SystemMessage.from(enhancedPrompt));
        }

        // 添加历史消息
        messages.addAll(history);

        return messages;
    }
}
