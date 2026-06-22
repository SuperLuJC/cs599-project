package com.ccsurvey.modules.agent.controller;

import com.ccsurvey.common.response.ApiResponse;
import com.ccsurvey.modules.agent.agent.AgentRequest;
import com.ccsurvey.modules.agent.agent.AgentResponse;
import com.ccsurvey.modules.agent.dto.*;
import com.ccsurvey.modules.agent.mq.AgentTask;
import com.ccsurvey.modules.agent.mq.AgentTaskProducer;
import com.ccsurvey.modules.agent.orchestrator.AgentOrchestrator;
import com.ccsurvey.modules.agent.service.AgentConversationService;
import com.ccsurvey.modules.agent.service.AgentStreamService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * Agent API 控制器
 * 基于 LangChain4j 重构后的 Agent 接口
 */
@Slf4j
@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
public class AgentController {

    private final AgentOrchestrator orchestrator;
    private final AgentStreamService streamService;
    private final AgentConversationService conversationService;
    private final AgentTaskProducer taskProducer;

    /**
     * 发送消息（非流式）
     */
    @PostMapping("/chat")
    public ApiResponse<AgentResponse> chat(@RequestBody ChatRequestDto request, HttpServletRequest httpRequest) {
        String userId = (String) httpRequest.getAttribute("userId");
        if (userId == null) {
            return ApiResponse.error(401, "未登录");
        }

        AgentRequest agentRequest = AgentRequest.of(
                request.getConversationId(),
                userId,
                request.getMessage()
        );
        agentRequest.setAgentType(request.getAgentType());

        try {
            AgentResponse response = orchestrator.execute(agentRequest);
            return ApiResponse.success(response);
        } catch (Exception e) {
            log.error("Agent chat error", e);
            return ApiResponse.error(500, "AI 服务暂时不可用: " + e.getMessage());
        }
    }

    /**
     * 发送消息（流式）
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(@RequestBody ChatRequestDto request, HttpServletRequest httpRequest) {
        String userId = (String) httpRequest.getAttribute("userId");
        if (userId == null) {
            SseEmitter emitter = new SseEmitter();
            try {
                emitter.send(SseEmitter.event().data(StreamEvent.error("未登录")));
                emitter.complete();
            } catch (Exception ignored) {}
            return emitter;
        }

        request.setUserId(userId);
        return streamService.streamChat(request);
    }

    /**
     * 异步任务提交
     * 将任务发送到消息队列，返回任务ID
     */
    @PostMapping("/task")
    public ApiResponse<TaskSubmitResult> submitTask(@RequestBody ChatRequestDto request, HttpServletRequest httpRequest) {
        String userId = (String) httpRequest.getAttribute("userId");
        if (userId == null) {
            return ApiResponse.error(401, "未登录");
        }

        // 构建 AgentTask
        AgentTask task = AgentTask.withConversation(
                request.getConversationId(),
                userId,
                request.getMessage()
        );
        task.setAgentType(request.getAgentType());

        // 发送到消息队列
        String taskId = taskProducer.sendTask(task);

        TaskSubmitResult result = new TaskSubmitResult();
        result.setTaskId(taskId);
        result.setConversationId(task.getConversationId());

        return ApiResponse.success(result);
    }

    /**
     * 获取会话列表
     */
    @GetMapping("/conversations")
    public ApiResponse<List<ConversationInfo>> getConversations(HttpServletRequest httpRequest) {
        String userId = (String) httpRequest.getAttribute("userId");
        if (userId == null) {
            return ApiResponse.error(401, "未登录");
        }

        List<ConversationInfo> conversations = conversationService.getConversations(userId);
        return ApiResponse.success(conversations);
    }

    /**
     * 获取会话历史
     */
    @GetMapping("/conversations/{conversationId}/messages")
    public ApiResponse<List<MessageInfo>> getConversationHistory(
            @PathVariable String conversationId,
            HttpServletRequest httpRequest) {
        String userId = (String) httpRequest.getAttribute("userId");
        if (userId == null) {
            return ApiResponse.error(401, "未登录");
        }

        List<MessageInfo> messages = conversationService.getConversationHistory(conversationId, userId);
        return ApiResponse.success(messages);
    }

    /**
     * 删除会话
     */
    @DeleteMapping("/conversations/{conversationId}")
    public ApiResponse<Void> deleteConversation(
            @PathVariable String conversationId,
            HttpServletRequest httpRequest) {
        String userId = (String) httpRequest.getAttribute("userId");
        if (userId == null) {
            return ApiResponse.error(401, "未登录");
        }

        conversationService.deleteConversation(conversationId, userId);
        return ApiResponse.success(null);
    }

    /**
     * 更新会话标题
     */
    @PutMapping("/conversations/{conversationId}/title")
    public ApiResponse<Void> updateTitle(
            @PathVariable String conversationId,
            @RequestBody UpdateTitleRequest request,
            HttpServletRequest httpRequest) {
        String userId = (String) httpRequest.getAttribute("userId");
        if (userId == null) {
            return ApiResponse.error(401, "未登录");
        }

        // 验证标题
        String title = request.getTitle();
        if (title == null || title.trim().isEmpty()) {
            return ApiResponse.error(400, "标题不能为空");
        }
        if (title.length() > 50) {
            return ApiResponse.error(400, "标题长度不能超过50个字符");
        }

        boolean success = conversationService.updateTitle(conversationId, title.trim(), userId);
        if (!success) {
            return ApiResponse.error(404, "会话不存在或无权限");
        }
        return ApiResponse.success(null);
    }

    /**
     * 聊天请求 DTO（用于 API 输入）
     */
    @Data
    public static class ChatRequestDto {
        private String conversationId;
        private String userId;
        private String agentType = "general";
        private String message;
    }

    /**
     * 任务提交结果
     */
    @Data
    public static class TaskSubmitResult {
        private String taskId;
        private String conversationId;
    }

    /**
     * 更新标题请求
     */
    @Data
    public static class UpdateTitleRequest {
        private String title;
    }
}
