package com.ccsurvey.modules.agent.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ccsurvey.modules.agent.entity.AiConversation;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * AI 会话 Mapper
 */
@Mapper
public interface AiConversationRepository extends BaseMapper<AiConversation> {

    default List<AiConversation> findByUserId(String userId) {
        return selectList(new LambdaQueryWrapper<AiConversation>()
                .eq(AiConversation::getUserId, userId)
                .orderByDesc(AiConversation::getUpdatedAt));
    }

    default AiConversation findById(String id) {
        return selectById(id);
    }
}