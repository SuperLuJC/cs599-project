package com.ccsurvey.modules.agent.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ccsurvey.modules.agent.entity.AiMessage;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * AI 消息 Mapper
 */
@Mapper
public interface AiMessageRepository extends BaseMapper<AiMessage> {

    default List<AiMessage> findByConversationId(String conversationId) {
        return selectList(new LambdaQueryWrapper<AiMessage>()
                .eq(AiMessage::getConversationId, conversationId)
                .orderByAsc(AiMessage::getSeq)
                .orderByAsc(AiMessage::getCreatedAt));
    }

    default void deleteByConversationId(String conversationId) {
        delete(new LambdaQueryWrapper<AiMessage>()
                .eq(AiMessage::getConversationId, conversationId));
    }
}