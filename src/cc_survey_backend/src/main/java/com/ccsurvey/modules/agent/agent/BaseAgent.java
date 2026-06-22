package com.ccsurvey.modules.agent.agent;

import com.ccsurvey.modules.agent.config.AgentProperties;
import com.ccsurvey.modules.agent.memory.AgentMemory;
import com.ccsurvey.modules.agent.protocol.ReActProtocol;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Agent 基类
 * 实现 ReAct (Reasoning + Acting) 循环
 */
@Slf4j
@Getter
public abstract class BaseAgent implements Agent {

    protected final ChatModel chatModel;
    protected final AgentMemory memory;
    protected final AgentProperties agentProperties;
    protected final ReActProtocol reActProtocol;

    protected BaseAgent(ChatModel chatModel,
                        AgentMemory memory,
                        AgentProperties agentProperties) {
        this.chatModel = chatModel;
        this.memory = memory;
        this.agentProperties = agentProperties;
        this.reActProtocol = new ReActProtocol();
    }

    @Override
    public AgentResponse execute(AgentRequest request, AgentContext context) {
        log.info("Executing agent: {} for conversation: {}", getName(), request.getConversationId());

        try {
            // 1. 加载历史记忆
            List<ChatMessage> history = loadHistory(request.getConversationId());

            // 2. 构建消息列表
            List<ChatMessage> messages = buildMessages(history, request, context);

            // 3. 执行 ReAct 循环
            String response = executeReActLoop(messages, context);

            // 4. 保存记忆
            saveMemory(request.getConversationId(), request.getMessage(), response);

            return AgentResponse.success(request.getConversationId(), response);

        } catch (Exception e) {
            log.error("Agent execution failed: {}", e.getMessage(), e);
            return AgentResponse.error("Agent execution failed: " + e.getMessage());
        }
    }

    /**
     * 加载历史记忆
     */
    protected List<ChatMessage> loadHistory(String conversationId) {
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
    protected List<ChatMessage> buildMessages(List<ChatMessage> history,
                                               AgentRequest request,
                                               AgentContext context) {
        List<ChatMessage> messages = new ArrayList<>();

        // 添加系统提示
        messages.add(SystemMessage.from(getSystemPrompt()));

        // 添加历史消息
        messages.addAll(history);

        // 添加用户消息
        messages.add(UserMessage.from(request.getMessage()));

        return messages;
    }

    /**
     * 执行 ReAct 循环
     * Thought -> Action -> Observation 循环直到得到最终答案
     */
    protected String executeReActLoop(List<ChatMessage> messages, AgentContext context) {
        int maxIterations = agentProperties.getOrchestration().getMaxIterations();
        int iteration = 0;
        StringBuilder fullResponse = new StringBuilder();

        while (iteration < maxIterations) {
            iteration++;
            context.setCurrentIteration(iteration);
            log.debug("ReAct iteration {}/{}", iteration, maxIterations);

            // 调用 LLM - 使用新的 API
            ChatRequest chatRequest = ChatRequest.builder()
                    .messages(messages)
                    .build();

            ChatResponse response = chatModel.chat(chatRequest);

            AiMessage aiMessage = response.aiMessage();
            String content = aiMessage.text();

            if (content != null && !content.isEmpty()) {
                fullResponse.append(content);
            }

            // 检查是否有工具调用
            if (aiMessage.hasToolExecutionRequests()) {
                log.debug("LLM requested tool execution: {} tools",
                        aiMessage.toolExecutionRequests().size());

                // 处理工具调用
                boolean allToolsExecuted = processToolCalls(aiMessage, messages, context);

                if (!allToolsExecuted) {
                    log.warn("Some tool executions failed");
                }

                // 继续循环，让 LLM 处理工具结果
                continue;
            }

            // 没有工具调用，返回最终答案
            log.debug("ReAct loop completed after {} iterations", iteration);
            return fullResponse.toString();
        }

        // 达到最大迭代次数
        log.warn("Reached max iterations: {}", maxIterations);
        return fullResponse.toString() + "\n\n(注：处理过程较复杂，已达到最大迭代次数)";
    }

    /**
     * 处理工具调用
     */
    protected boolean processToolCalls(AiMessage aiMessage,
                                        List<ChatMessage> messages,
                                        AgentContext context) {
        // 将 AI 消息添加到历史
        messages.add(aiMessage);

        // 执行每个工具调用
        for (var toolRequest : aiMessage.toolExecutionRequests()) {
            String toolName = toolRequest.name();
            String toolArgs = toolRequest.arguments();

            log.info("Executing tool: {} with args: {}", toolName, toolArgs);

            try {
                // 执行工具
                String result = executeTool(toolName, toolArgs, context);

                // 添加工具结果到消息
                messages.add(dev.langchain4j.data.message.ToolExecutionResultMessage.from(
                        toolRequest.id(),
                        toolName,
                        result
                ));

                log.debug("Tool {} executed successfully", toolName);

            } catch (Exception e) {
                log.error("Tool execution failed: {}", e.getMessage());

                // 添加错误结果
                messages.add(dev.langchain4j.data.message.ToolExecutionResultMessage.from(
                        toolRequest.id(),
                        toolName,
                        "Error: " + e.getMessage()
                ));

                return false;
            }
        }

        return true;
    }

    /**
     * 执行工具
     * 子类可以重写此方法来实现自定义的工具执行逻辑
     */
    protected String executeTool(String toolName, String arguments, AgentContext context) {
        // 默认实现：返回未实现错误
        return "Tool '" + toolName + "' is not implemented in this agent.";
    }

    /**
     * 保存记忆
     */
    protected void saveMemory(String conversationId, String userMessage, String assistantResponse) {
        if (conversationId == null) {
            return;
        }

        // 保存用户消息
        memory.addMessage(conversationId, UserMessage.from(userMessage));

        // 保存助手响应
        memory.addMessage(conversationId, AiMessage.from(assistantResponse));

        log.debug("Saved messages to memory for conversation: {}", conversationId);
    }

    @Override
    public List<ToolSpecification> getTools() {
        // 默认返回空列表，子类可以重写
        return List.of();
    }

    @Override
    public boolean canHandle(com.ccsurvey.modules.agent.intent.Intent intent) {
        // 默认实现：检查意图对应的 Agent 类型
        return intent.getAgentType().equals(getType());
    }
}
