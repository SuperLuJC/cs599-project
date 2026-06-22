package com.ccsurvey.modules.agent.service;

import com.ccsurvey.modules.agent.dto.ConversationInfo;
import com.ccsurvey.modules.agent.dto.MessageInfo;
import com.ccsurvey.modules.agent.entity.AiConversation;
import com.ccsurvey.modules.agent.entity.AiMessage;
import com.ccsurvey.modules.agent.repository.AiConversationRepository;
import com.ccsurvey.modules.agent.repository.AiMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;

/**
 * Agent 会话服务
 * 处理会话的 CRUD 操作
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentConversationService {

    private final AiConversationRepository conversationRepository;
    private final AiMessageRepository messageRepository;

    /**
     * 创建新会话
     */
    public AiConversation createConversation(String userId, String agentType, String title) {
        AiConversation conversation = new AiConversation();
        conversation.setUserId(userId);
        conversation.setTitle(title != null ? title : "新对话");
        conversation.setAgentType(agentType);
        conversation.setStatus(1);
        conversation.setContext(new HashMap<>());

        conversationRepository.insert(conversation);
        return conversation;
    }

    /**
     * 获取或创建会话
     */
    public AiConversation getOrCreateConversation(String conversationId, String userId, String agentType) {
        if (conversationId != null && !conversationId.isEmpty()) {
            AiConversation existing = conversationRepository.findById(conversationId);
            if (existing != null) {
                return existing;
            }
        }

        return createConversation(userId, agentType, "新对话");
    }

    /**
     * 获取会话
     */
    public AiConversation getConversation(String conversationId) {
        return conversationRepository.findById(conversationId);
    }

    /**
     * 更新会话
     */
    public void updateConversation(AiConversation conversation) {
        conversationRepository.updateById(conversation);
    }

    /**
     * 更新会话标题
     */
    public void updateTitle(String conversationId, String title) {
        AiConversation conversation = conversationRepository.findById(conversationId);
        if (conversation != null) {
            conversation.setTitle(title);
            conversationRepository.updateById(conversation);
        }
    }

    /**
     * 更新会话标题（带用户验证）
     */
    public boolean updateTitle(String conversationId, String title, String userId) {
        AiConversation conversation = conversationRepository.findById(conversationId);
        if (conversation != null && conversation.getUserId().equals(userId)) {
            conversation.setTitle(title);
            conversationRepository.updateById(conversation);
            return true;
        }
        return false;
    }

    /**
     * 保存消息
     */
    public void saveMessage(String conversationId, String role, String content) {
        AiMessage message = new AiMessage();
        message.setConversationId(conversationId);
        message.setRole(role);
        message.setContent(content);
        messageRepository.insert(message);
    }

    /**
     * 获取用户的会话列表
     */
    public List<ConversationInfo> getConversations(String userId) {
        List<AiConversation> conversations = conversationRepository.findByUserId(userId);
        return conversations.stream()
                .map(c -> {
                    ConversationInfo info = new ConversationInfo();
                    info.setId(c.getId());
                    info.setTitle(c.getTitle());
                    info.setAgentType(c.getAgentType());
                    info.setCreatedAt(c.getCreatedAt().toString());
                    return info;
                })
                .toList();
    }

    /**
     * 获取会话历史
     */
    public List<MessageInfo> getConversationHistory(String conversationId, String userId) {
        AiConversation conversation = conversationRepository.findById(conversationId);
        if (conversation == null || !conversation.getUserId().equals(userId)) {
            return List.of();
        }

        List<AiMessage> messages = messageRepository.findByConversationId(conversationId);
        return messages.stream()
                .map(m -> {
                    MessageInfo info = new MessageInfo();
                    info.setRole(m.getRole());
                    info.setContent(m.getContent());
                    info.setCreatedAt(m.getCreatedAt().toString());
                    return info;
                })
                .toList();
    }

    /**
     * 删除会话
     */
    @Transactional
    public void deleteConversation(String conversationId, String userId) {
        AiConversation conversation = conversationRepository.findById(conversationId);
        if (conversation != null && conversation.getUserId().equals(userId)) {
            messageRepository.deleteByConversationId(conversationId);
            conversationRepository.deleteById(conversationId);
        }
    }

    /**
     * 根据消息生成标题
     * 智能提取用户消息中的关键词作为标题
     */
    public String generateTitle(String userMessage) {
        if (userMessage == null || userMessage.isEmpty()) {
            return "新对话";
        }

        String lowerMessage = userMessage.toLowerCase();

        // 检测问候语，直接返回"新对话"
        if (lowerMessage.matches("^(hi|hello|你好|您好|hey|嗨).*$")) {
            return "新对话";
        }

        // 清理消息前缀（礼貌用语、指令词等）
        String cleaned = userMessage
                .replaceAll("(?i)^(please|请|can you|你能|帮我|help|我想|i want to|could you|麻烦|麻烦你)\\s*", "")
                .replaceAll("(?i)^(创建|新建|生成|制作|设计|帮我创建|帮我生成|帮我设计)\\s*", "")
                .trim();

        // 根据意图确定前缀
        String prefix = "";
        if (containsAny(lowerMessage, "创建", "新建", "生成", "制作", "设计", "build", "create", "make")) {
            if (containsAny(lowerMessage, "问卷", "调查", "表单", "survey", "questionnaire", "form")) {
                prefix = "[问卷] ";
                // 移除"问卷"等后缀词，避免重复
                cleaned = cleaned.replaceAll("(?i)(问卷|调查|表单)\\s*$", "").trim();
            }
        } else if (containsAny(lowerMessage, "分析", "统计", "查看数据", "数据", "analyze", "data", "statistics")) {
            prefix = "[数据] ";
        } else if (containsAny(lowerMessage, "日志", "操作记录", "log", "activity")) {
            prefix = "[日志] ";
        }

        // 提取核心内容作为标题
        if (cleaned.length() > 0) {
            // 限制长度，保留关键信息
            String title = cleaned.length() > 20 ? cleaned.substring(0, 20) + "..." : cleaned;
            return prefix + title;
        }

        return "新对话";
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword.toLowerCase())) {
                return true;
            }
        }
        return false;
    }
}
