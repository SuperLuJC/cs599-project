package com.ccsurvey.modules.agent.memory;

import com.ccsurvey.modules.agent.config.AgentProperties;
import dev.langchain4j.data.message.ChatMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 组合记忆实现
 * 结合短期记忆（Redis）和长期记忆（MySQL）
 */
@Slf4j
@Component
@Primary
@RequiredArgsConstructor
public class CompositeMemory implements AgentMemory {

    private final ShortTermMemory shortTermMemory;
    private final LongTermMemory longTermMemory;
    private final AgentProperties agentProperties;

    @Override
    public void addMessage(String conversationId, ChatMessage message) {
        // 同时写入短期和长期记忆
        if (agentProperties.getMemory().getShortTerm().getEnabled()) {
            shortTermMemory.addMessage(conversationId, message);
        }
        if (agentProperties.getMemory().getLongTerm().getEnabled()) {
            longTermMemory.addMessage(conversationId, message);
        }
    }

    @Override
    public List<ChatMessage> getMessages(String conversationId) {
        // 优先从短期记忆获取，如果不足则补充长期记忆
        List<ChatMessage> shortTermMessages = shortTermMemory.getMessages(conversationId);
        int windowSize = agentProperties.getMemory().getShortTerm().getWindowSize();

        if (shortTermMessages.size() >= windowSize) {
            return shortTermMessages;
        }

        // 从长期记忆补充
        List<ChatMessage> longTermMessages = longTermMemory.getMessages(conversationId);

        if (longTermMessages.isEmpty()) {
            return shortTermMessages;
        }

        // 合并消息，避免重复
        return mergeMessages(longTermMessages, shortTermMessages, windowSize);
    }

    @Override
    public List<ChatMessage> getRecentMessages(String conversationId, int limit) {
        // 优先从短期记忆获取
        List<ChatMessage> shortTermMessages = shortTermMemory.getRecentMessages(conversationId, limit);

        if (shortTermMessages.size() >= limit) {
            return shortTermMessages;
        }

        // 从长期记忆补充
        List<ChatMessage> longTermMessages = longTermMemory.getRecentMessages(conversationId, limit);
        return mergeMessages(longTermMessages, shortTermMessages, limit);
    }

    @Override
    public void clear(String conversationId) {
        shortTermMemory.clear(conversationId);
        // 长期记忆不自动清除，由业务层决定
    }

    @Override
    public void setContext(String conversationId, String key, Object value) {
        // 上下文只存储在短期记忆中
        shortTermMemory.setContext(conversationId, key, value);
    }

    @Override
    public <T> T getContext(String conversationId, String key) {
        return shortTermMemory.getContext(conversationId, key);
    }

    @Override
    public Map<String, Object> getAllContext(String conversationId) {
        return shortTermMemory.getAllContext(conversationId);
    }

    @Override
    public void clearContext(String conversationId, String key) {
        shortTermMemory.clearContext(conversationId, key);
    }

    @Override
    public boolean exists(String conversationId) {
        return shortTermMemory.exists(conversationId) || longTermMemory.exists(conversationId);
    }

    /**
     * 合并消息列表
     */
    private List<ChatMessage> mergeMessages(List<ChatMessage> longTermMessages,
                                              List<ChatMessage> shortTermMessages,
                                              int limit) {
        List<ChatMessage> result = new ArrayList<>();

        // 添加长期记忆的消息
        if (longTermMessages != null) {
            result.addAll(longTermMessages);
        }

        // 添加短期记忆的消息（避免重复）
        if (shortTermMessages != null) {
            for (ChatMessage message : shortTermMessages) {
                if (!containsMessage(result, message)) {
                    result.add(message);
                }
            }
        }

        // 如果超过限制，截取最近的消息
        if (result.size() > limit) {
            return result.subList(result.size() - limit, result.size());
        }

        return result;
    }

    /**
     * 检查消息列表是否包含某消息
     */
    private boolean containsMessage(List<ChatMessage> messages, ChatMessage target) {
        if (messages == null || target == null) {
            return false;
        }

        String targetContent = extractContent(target);
        for (ChatMessage message : messages) {
            if (message.type() == target.type() &&
                extractContent(message).equals(targetContent)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 提取消息内容
     */
    private String extractContent(ChatMessage message) {
        if (message instanceof dev.langchain4j.data.message.SystemMessage) {
            return ((dev.langchain4j.data.message.SystemMessage) message).text();
        } else if (message instanceof dev.langchain4j.data.message.UserMessage) {
            return ((dev.langchain4j.data.message.UserMessage) message).singleText();
        } else if (message instanceof dev.langchain4j.data.message.AiMessage) {
            return ((dev.langchain4j.data.message.AiMessage) message).text();
        }
        return message.toString();
    }
}
