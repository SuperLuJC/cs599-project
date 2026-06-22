package com.ccsurvey.modules.agent.memory;

import com.alibaba.fastjson2.JSON;
import com.ccsurvey.modules.agent.entity.AiConversation;
import com.ccsurvey.modules.agent.entity.AiMessage;
import com.ccsurvey.modules.agent.repository.AiConversationRepository;
import com.ccsurvey.modules.agent.repository.AiMessageRepository;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 长期记忆实现
 * 使用 MySQL 持久化所有消息历史
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LongTermMemory implements AgentMemory {

    private final AiMessageRepository messageRepository;
    private final AiConversationRepository conversationRepository;

    @Override
    public void addMessage(String conversationId, ChatMessage message) {
        if (conversationId == null || message == null) {
            return;
        }

        // 确保父会话存在
        ensureConversationExists(conversationId);

        AiMessage entity = new AiMessage();
        entity.setConversationId(conversationId);

        // 统一 role 映射：usermessage -> user, aimessage -> ai
        String role = message.type().name().toLowerCase();
        if (role.equals("usermessage")) {
            role = "user";
        } else if (role.equals("aimessage")) {
            role = "ai";
        }
        entity.setRole(role);

        // 提取消息内容
        String content = extractContent(message);
        entity.setContent(content);

        // 处理工具调用 - 检查是否是 LangChain4j 的 AiMessage
        if (message instanceof dev.langchain4j.data.message.AiMessage) {
            dev.langchain4j.data.message.AiMessage aiMessage =
                    (dev.langchain4j.data.message.AiMessage) message;
            if (aiMessage.toolExecutionRequests() != null && !aiMessage.toolExecutionRequests().isEmpty()) {
                // 转换工具调用为实体格式
                entity.setToolCalls(convertToolCalls(aiMessage.toolExecutionRequests()));
            }
        }

        messageRepository.insert(entity);
        log.debug("Added message to long-term memory: conversationId={}, role={}",
                conversationId, role);
    }

    /**
     * 确保父会话记录存在
     */
    private void ensureConversationExists(String conversationId) {
        AiConversation existing = conversationRepository.findById(conversationId);
        if (existing == null) {
            AiConversation conversation = new AiConversation();
            conversation.setId(conversationId);
            conversation.setUserId("system");
            conversation.setTitle("Auto-created conversation");
            conversation.setAgentType("general");
            conversation.setStatus(1);
            conversationRepository.insert(conversation);
            log.debug("Auto-created conversation: {}", conversationId);
        }
    }

    @Override
    public List<ChatMessage> getMessages(String conversationId) {
        if (conversationId == null) {
            return Collections.emptyList();
        }

        List<AiMessage> messages = messageRepository.findByConversationId(conversationId);
        if (messages == null || messages.isEmpty()) {
            return Collections.emptyList();
        }

        List<ChatMessage> result = new ArrayList<>();
        for (AiMessage entity : messages) {
            ChatMessage message = convertToChatMessage(entity);
            if (message != null) {
                result.add(message);
            }
        }

        return result;
    }

    @Override
    public List<ChatMessage> getRecentMessages(String conversationId, int limit) {
        if (conversationId == null || limit <= 0) {
            return Collections.emptyList();
        }

        List<ChatMessage> allMessages = getMessages(conversationId);
        if (allMessages.size() <= limit) {
            return allMessages;
        }

        return allMessages.subList(allMessages.size() - limit, allMessages.size());
    }

    @Override
    public void clear(String conversationId) {
        if (conversationId == null) {
            return;
        }

        // 长期记忆不直接删除，由业务层决定是否删除
        log.debug("Long-term memory clear called for conversationId={}", conversationId);
    }

    @Override
    public void setContext(String conversationId, String key, Object value) {
        // 长期记忆不存储上下文，由 CompositeMemory 委托给 ShortTermMemory
        log.debug("Long-term memory setContext called - delegating to short-term memory");
    }

    @Override
    public <T> T getContext(String conversationId, String key) {
        // 长期记忆不存储上下文
        return null;
    }

    @Override
    public Map<String, Object> getAllContext(String conversationId) {
        return Collections.emptyMap();
    }

    @Override
    public void clearContext(String conversationId, String key) {
        // 长期记忆不存储上下文
    }

    @Override
    public boolean exists(String conversationId) {
        if (conversationId == null) {
            return false;
        }

        List<AiMessage> messages = messageRepository.findByConversationId(conversationId);
        return messages != null && !messages.isEmpty();
    }

    /**
     * 提取消息内容
     */
    private String extractContent(ChatMessage message) {
        if (message instanceof SystemMessage) {
            return ((SystemMessage) message).text();
        } else if (message instanceof UserMessage) {
            return ((UserMessage) message).singleText();
        } else if (message instanceof dev.langchain4j.data.message.AiMessage) {
            return ((dev.langchain4j.data.message.AiMessage) message).text();
        } else if (message instanceof ToolExecutionResultMessage) {
            return ((ToolExecutionResultMessage) message).text();
        }
        return message.toString();
    }

    /**
     * 转换工具调用列表
     */
    private List<AiMessage.ToolCall> convertToolCalls(
            List<ToolExecutionRequest> requests) {
        List<AiMessage.ToolCall> result = new ArrayList<>();
        for (ToolExecutionRequest request : requests) {
            AiMessage.ToolCall toolCall = new AiMessage.ToolCall();
            toolCall.setId(request.id());
            toolCall.setName(request.name());
            // arguments 是 JSON 字符串，需要转换为 Map
            if (request.arguments() != null && !request.arguments().isEmpty()) {
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> argsMap = JSON.parseObject(request.arguments(), Map.class);
                    toolCall.setArguments(argsMap);
                } catch (Exception e) {
                    log.warn("Failed to parse tool arguments: {}", e.getMessage());
                }
            }
            result.add(toolCall);
        }
        return result;
    }

    /**
     * 转换实体为 ChatMessage
     */
    private ChatMessage convertToChatMessage(AiMessage entity) {
        if (entity == null || entity.getRole() == null) {
            return null;
        }

        String role = entity.getRole().toLowerCase();
        String content = entity.getContent();

        try {
            switch (role) {
                case "system":
                    return SystemMessage.from(content);
                case "user":
                    return UserMessage.from(content);
                case "assistant":
                case "ai":
                    // 如果有工具调用，需要构建带工具调用的 AiMessage
                    if (entity.getToolCalls() != null && !entity.getToolCalls().isEmpty()) {
                        List<ToolExecutionRequest> requests = new ArrayList<>();
                        for (AiMessage.ToolCall tc : entity.getToolCalls()) {
                            // 将 Map 转换为 JSON 字符串
                            String argsJson = "";
                            if (tc.getArguments() != null) {
                                argsJson = JSON.toJSONString(tc.getArguments());
                            }
                            ToolExecutionRequest req = ToolExecutionRequest.builder()
                                    .id(tc.getId())
                                    .name(tc.getName())
                                    .arguments(argsJson)
                                    .build();
                            requests.add(req);
                        }
                        return dev.langchain4j.data.message.AiMessage.from(content, requests);
                    }
                    return dev.langchain4j.data.message.AiMessage.from(content);
                case "tool":
                    // 工具结果消息需要从元数据中获取 toolCallId
                    ToolExecutionRequest request = ToolExecutionRequest.builder()
                            .id("")
                            .name("")
                            .arguments("")
                            .build();
                    return ToolExecutionResultMessage.from(request, content);
                default:
                    log.warn("Unknown message role: {}", role);
                    return null;
            }
        } catch (Exception e) {
            log.warn("Failed to convert message: {}", e.getMessage());
            return null;
        }
    }
}
