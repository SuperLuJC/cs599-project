package com.ccsurvey.modules.agent.memory;

import com.alibaba.fastjson2.JSON;
import com.ccsurvey.modules.agent.config.AgentProperties;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 短期记忆实现
 * 使用 Redis 存储最近的消息，支持滑动窗口
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ShortTermMemory implements AgentMemory {

    private final StringRedisTemplate redisTemplate;
    private final AgentProperties agentProperties;

    private static final String MESSAGE_KEY_PREFIX = "agent:memory:messages:";
    private static final String CONTEXT_KEY_PREFIX = "agent:memory:context:";

    @Override
    public void addMessage(String conversationId, ChatMessage message) {
        if (conversationId == null || message == null) {
            return;
        }

        String key = MESSAGE_KEY_PREFIX + conversationId;
        String messageJson = serializeMessage(message);

        AgentProperties.ShortTermConfig config = agentProperties.getMemory().getShortTerm();
        int windowSize = config.getWindowSize();

        // 添加到列表尾部
        redisTemplate.opsForList().rightPush(key, messageJson);

        // 保持窗口大小，移除多余的消息
        Long size = redisTemplate.opsForList().size(key);
        if (size != null && size > windowSize) {
            redisTemplate.opsForList().trim(key, -windowSize, -1);
        }

        // 设置过期时间
        redisTemplate.expire(key, Duration.ofMinutes(config.getTtlMinutes()));

        log.debug("Added message to short-term memory: conversationId={}, type={}",
                conversationId, message.type());
    }

    @Override
    public List<ChatMessage> getMessages(String conversationId) {
        if (conversationId == null) {
            return Collections.emptyList();
        }

        String key = MESSAGE_KEY_PREFIX + conversationId;
        List<String> messages = redisTemplate.opsForList().range(key, 0, -1);

        if (messages == null || messages.isEmpty()) {
            return Collections.emptyList();
        }

        List<ChatMessage> result = new ArrayList<>();
        for (String messageJson : messages) {
            ChatMessage message = deserializeMessage(messageJson);
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

        String key = MESSAGE_KEY_PREFIX + conversationId;
        List<String> messages = redisTemplate.opsForList().range(key, -limit, -1);

        if (messages == null || messages.isEmpty()) {
            return Collections.emptyList();
        }

        List<ChatMessage> result = new ArrayList<>();
        for (String messageJson : messages) {
            ChatMessage message = deserializeMessage(messageJson);
            if (message != null) {
                result.add(message);
            }
        }

        return result;
    }

    @Override
    public void clear(String conversationId) {
        if (conversationId == null) {
            return;
        }

        String messageKey = MESSAGE_KEY_PREFIX + conversationId;
        String contextKey = CONTEXT_KEY_PREFIX + conversationId;

        redisTemplate.delete(messageKey);
        redisTemplate.delete(contextKey);

        log.debug("Cleared short-term memory: conversationId={}", conversationId);
    }

    @Override
    public void setContext(String conversationId, String key, Object value) {
        if (conversationId == null || key == null) {
            return;
        }

        String contextKey = CONTEXT_KEY_PREFIX + conversationId;
        String field = key;

        if (value == null) {
            redisTemplate.opsForHash().delete(contextKey, field);
        } else {
            String valueJson = JSON.toJSONString(value);
            redisTemplate.opsForHash().put(contextKey, field, valueJson);

            AgentProperties.ShortTermConfig config = agentProperties.getMemory().getShortTerm();
            redisTemplate.expire(contextKey, Duration.ofMinutes(config.getTtlMinutes()));
        }

        log.debug("Set context: conversationId={}, key={}", conversationId, key);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getContext(String conversationId, String key) {
        if (conversationId == null || key == null) {
            return null;
        }

        String contextKey = CONTEXT_KEY_PREFIX + conversationId;
        Object value = redisTemplate.opsForHash().get(contextKey, key);

        if (value == null) {
            return null;
        }

        try {
            return (T) JSON.parseObject(value.toString(), Object.class);
        } catch (Exception e) {
            log.warn("Failed to parse context value: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public Map<String, Object> getAllContext(String conversationId) {
        if (conversationId == null) {
            return new HashMap<>();
        }

        String contextKey = CONTEXT_KEY_PREFIX + conversationId;
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(contextKey);

        if (entries == null || entries.isEmpty()) {
            return new HashMap<>();
        }

        Map<String, Object> result = new HashMap<>();
        for (Map.Entry<Object, Object> entry : entries.entrySet()) {
            String key = entry.getKey().toString();
            try {
                Object value = JSON.parseObject(entry.getValue().toString(), Object.class);
                result.put(key, value);
            } catch (Exception e) {
                result.put(key, entry.getValue());
            }
        }

        return result;
    }

    @Override
    public void clearContext(String conversationId, String key) {
        if (conversationId == null || key == null) {
            return;
        }

        String contextKey = CONTEXT_KEY_PREFIX + conversationId;
        redisTemplate.opsForHash().delete(contextKey, key);

        log.debug("Cleared context: conversationId={}, key={}", conversationId, key);
    }

    @Override
    public boolean exists(String conversationId) {
        if (conversationId == null) {
            return false;
        }

        String key = MESSAGE_KEY_PREFIX + conversationId;
        Long size = redisTemplate.opsForList().size(key);
        return size != null && size > 0;
    }

    /**
     * 序列化消息
     */
    private String serializeMessage(ChatMessage message) {
        Map<String, Object> map = new HashMap<>();
        map.put("type", message.type().name());

        switch (message.type()) {
            case SYSTEM:
                map.put("content", ((SystemMessage) message).text());
                break;
            case USER:
                map.put("content", ((UserMessage) message).singleText());
                break;
            case AI:
                AiMessage aiMessage = (AiMessage) message;
                map.put("content", aiMessage.text());
                if (aiMessage.toolExecutionRequests() != null && !aiMessage.toolExecutionRequests().isEmpty()) {
                    List<Map<String, Object>> toolCalls = new ArrayList<>();
                    for (ToolExecutionRequest req : aiMessage.toolExecutionRequests()) {
                        Map<String, Object> tc = new HashMap<>();
                        tc.put("id", req.id());
                        tc.put("name", req.name());
                        tc.put("arguments", req.arguments());
                        toolCalls.add(tc);
                    }
                    map.put("toolCalls", toolCalls);
                }
                break;
            case TOOL_EXECUTION_RESULT:
                ToolExecutionResultMessage toolResult = (ToolExecutionResultMessage) message;
                map.put("content", toolResult.text());
                map.put("toolCallId", toolResult.id());
                break;
            default:
                map.put("content", message.toString());
        }

        return JSON.toJSONString(map);
    }

    /**
     * 反序列化消息
     */
    @SuppressWarnings("unchecked")
    private ChatMessage deserializeMessage(String json) {
        try {
            Map<String, Object> map = JSON.parseObject(json, Map.class);
            String type = (String) map.get("type");
            String content = (String) map.get("content");

            if (type == null) {
                return null;
            }

            switch (type) {
                case "SYSTEM":
                    return SystemMessage.from(content);
                case "USER":
                    return UserMessage.from(content);
                case "AI":
                    // 处理工具调用
                    List<Map<String, Object>> toolCalls = (List<Map<String, Object>>) map.get("toolCalls");
                    if (toolCalls != null && !toolCalls.isEmpty()) {
                        List<ToolExecutionRequest> requests = new ArrayList<>();
                        for (Map<String, Object> tc : toolCalls) {
                            ToolExecutionRequest req = ToolExecutionRequest.builder()
                                    .id((String) tc.get("id"))
                                    .name((String) tc.get("name"))
                                    .arguments((String) tc.get("arguments"))
                                    .build();
                            requests.add(req);
                        }
                        return AiMessage.from(content, requests);
                    }
                    return AiMessage.from(content);
                case "TOOL_EXECUTION_RESULT":
                    String toolCallId = (String) map.get("toolCallId");
                    // 创建一个简单的 ToolExecutionRequest 用于构建消息
                    ToolExecutionRequest request = ToolExecutionRequest.builder()
                            .id(toolCallId)
                            .name("")
                            .arguments("")
                            .build();
                    return ToolExecutionResultMessage.from(request, content);
                default:
                    log.warn("Unknown message type: {}", type);
                    return null;
            }
        } catch (Exception e) {
            log.warn("Failed to deserialize message: {}", e.getMessage());
            return null;
        }
    }
}
