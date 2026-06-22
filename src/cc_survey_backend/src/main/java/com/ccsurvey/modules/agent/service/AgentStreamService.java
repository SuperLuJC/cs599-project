package com.ccsurvey.modules.agent.service;

import com.ccsurvey.modules.agent.agent.AgentRequest;
import com.ccsurvey.modules.agent.controller.AgentController.ChatRequestDto;
import com.ccsurvey.modules.agent.dto.StreamEvent;
import com.ccsurvey.modules.agent.orchestrator.StreamingAgentOrchestrator;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

/**
 * Agent 流式服务 - 重构版
 *
 * 使用 StreamingAgentOrchestrator 进行流式对话
 * 保留意图识别、记忆管理、工具调用等功能
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentStreamService {

    private final StreamingAgentOrchestrator streamingOrchestrator;

    /**
     * 流式聊天
     */
    public SseEmitter streamChat(ChatRequestDto request) {
        SseEmitter emitter = new SseEmitter(180000L); // 3分钟超时

        // 设置超时和完成回调
        emitter.onTimeout(() -> {
            log.warn("SSE emitter timeout for conversation: {}", request.getConversationId());
        });
        emitter.onCompletion(() -> {
            log.debug("SSE emitter completed for conversation: {}", request.getConversationId());
        });
        emitter.onError((error) -> {
            log.error("SSE emitter error for conversation: {}", request.getConversationId(), error);
        });

        // 构建 AgentRequest
        AgentRequest agentRequest = AgentRequest.of(
                request.getConversationId(),
                request.getUserId(),
                request.getMessage()
        );
        agentRequest.setAgentType(request.getAgentType());

        // 使用 StreamingAgentOrchestrator 执行流式对话
        streamingOrchestrator.executeStream(agentRequest, emitter);

        return emitter;
    }
}