package com.ccsurvey.modules.agent;

import com.ccsurvey.modules.agent.agent.*;
import com.ccsurvey.modules.agent.config.AgentProperties;
import com.ccsurvey.modules.agent.intent.*;
import com.ccsurvey.modules.agent.memory.AgentMemory;
import com.ccsurvey.modules.agent.memory.CompositeMemory;
import com.ccsurvey.modules.agent.orchestrator.AgentCoordinator;
import com.ccsurvey.modules.agent.orchestrator.AgentOrchestrator;
import com.ccsurvey.modules.agent.orchestrator.IntentRouter;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Agent 模块全流程业务测试
 */
@SpringBootTest
@ActiveProfiles("dev")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AgentModuleIntegrationTest {

    @Autowired
    private AgentOrchestrator orchestrator;

    @Autowired
    private AgentCoordinator coordinator;

    @Autowired
    private IntentRouter intentRouter;

    @Autowired
    private AgentMemory memory;

    @Autowired
    private ChatModel chatModel;

    @Autowired
    private AgentProperties agentProperties;

    @Autowired
    private Map<String, Agent> agents;

    private static final String TEST_USER_ID = "test-user-001";
    private static String testConversationId;

    @BeforeEach
    void setUp() {
        // 使用 32 字符的 conversationId 以匹配数据库字段长度
        testConversationId = UUID.randomUUID().toString().replace("-", "").substring(0, 32);
    }

    // ==================== 配置测试 ====================

    @Test
    @Order(1)
    @DisplayName("1.1 验证 Agent 配置加载")
    void testAgentPropertiesLoaded() {
        assertNotNull(agentProperties, "AgentProperties 应该被注入");
        assertNotNull(agentProperties.getLlm(), "LLM 配置应该存在");
        assertNotNull(agentProperties.getMemory(), "Memory 配置应该存在");
        assertNotNull(agentProperties.getIntent(), "Intent 配置应该存在");
        assertNotNull(agentProperties.getOrchestration(), "Orchestration 配置应该存在");

        // 验证 LLM 配置
        assertEquals("xfyun", agentProperties.getLlm().getProvider());
        assertNotNull(agentProperties.getLlm().getXfyun().getBaseUrl());
        assertNotNull(agentProperties.getLlm().getXfyun().getModel());

        // 验证记忆配置
        assertTrue(agentProperties.getMemory().getShortTerm().getEnabled());
        assertEquals(20, agentProperties.getMemory().getShortTerm().getWindowSize());

        // 验证编排配置
        assertEquals(5, agentProperties.getOrchestration().getMaxIterations());

        System.out.println("✅ Agent 配置加载成功");
        System.out.println("   - LLM Provider: " + agentProperties.getLlm().getProvider());
        System.out.println("   - Model: " + agentProperties.getLlm().getXfyun().getModel());
        System.out.println("   - Memory Window: " + agentProperties.getMemory().getShortTerm().getWindowSize());
    }

    @Test
    @Order(2)
    @DisplayName("1.2 验证 ChatModel Bean 注入")
    void testChatModelInjected() {
        assertNotNull(chatModel, "ChatModel 应该被注入");
        System.out.println("✅ ChatModel 注入成功: " + chatModel.getClass().getSimpleName());
    }

    // ==================== Agent 注册测试 ====================

    @Test
    @Order(10)
    @DisplayName("2.1 验证 Agent 注册")
    void testAgentsRegistered() {
        assertNotNull(agents, "Agent Map 应该被注入");
        assertFalse(agents.isEmpty(), "应该有 Agent 被注册");

        System.out.println("✅ 已注册的 Agent:");
        agents.forEach((name, agent) -> {
            System.out.println("   - " + name + ": " + agent.getDescription());
            assertNotNull(agent.getName(), "Agent 名称不能为空");
            assertNotNull(agent.getType(), "Agent 类型不能为空");
        });

        // 验证关键 Agent 存在
        assertTrue(agents.containsKey("routerAgent"), "RouterAgent 应该存在");
    }

    @Test
    @Order(11)
    @DisplayName("2.2 验证 Agent 能力声明")
    void testAgentCapabilities() {
        Agent routerAgent = agents.get("routerAgent");
        assertNotNull(routerAgent);

        // 测试 canHandle 方法
        assertTrue(routerAgent.canHandle(Intent.GENERAL_CHAT), "RouterAgent 应该能处理 GENERAL_CHAT");
        assertTrue(routerAgent.canHandle(Intent.CREATE_SURVEY), "RouterAgent 作为后备应该能处理所有意图");

        System.out.println("✅ Agent 能力声明验证通过");
    }

    // ==================== 意图识别测试 ====================

    @Test
    @Order(20)
    @DisplayName("3.1 规则基础意图识别 - 创建问卷")
    void testRuleBasedIntent_CreateSurvey() {
        String message = "帮我创建一个客户满意度调查问卷";

        IntentResult result = intentRouter.route(message, null);

        assertNotNull(result, "意图识别结果不能为空");
        assertNotNull(result.getPrimaryIntent(), "主要意图不能为空");
        assertEquals(Intent.CREATE_SURVEY, result.getPrimaryIntent(), "应该识别为创建问卷意图");
        assertTrue(result.getConfidence() > 0.5f, "置信度应该大于 0.5");

        System.out.println("✅ 意图识别成功: " + result.getPrimaryIntent());
        System.out.println("   - 置信度: " + result.getConfidence());
        System.out.println("   - Agent 类型: " + result.getPrimaryIntent().getAgentType());
    }

    @Test
    @Order(21)
    @DisplayName("3.2 规则基础意图识别 - 数据分析")
    void testRuleBasedIntent_AnalyzeData() {
        String message = "帮我分析一下问卷的提交数据统计";

        IntentResult result = intentRouter.route(message, null);

        assertNotNull(result);
        assertEquals(Intent.ANALYZE_DATA, result.getPrimaryIntent(), "应该识别为数据分析意图");

        System.out.println("✅ 数据分析意图识别成功");
    }

    @Test
    @Order(22)
    @DisplayName("3.3 规则基础意图识别 - 日志查询")
    void testRuleBasedIntent_QueryLogs() {
        String message = "查看最近一周的操作日志";

        IntentResult result = intentRouter.route(message, null);

        assertNotNull(result);
        assertEquals(Intent.QUERY_LOGS, result.getPrimaryIntent(), "应该识别为日志查询意图");

        System.out.println("✅ 日志查询意图识别成功");
    }

    @Test
    @Order(23)
    @DisplayName("3.4 规则基础意图识别 - 通用对话")
    void testRuleBasedIntent_GeneralChat() {
        String message = "你好，今天天气怎么样？";

        IntentResult result = intentRouter.route(message, null);

        assertNotNull(result);
        assertEquals(Intent.GENERAL_CHAT, result.getPrimaryIntent(), "应该识别为通用对话意图");

        System.out.println("✅ 通用对话意图识别成功");
    }

    @Test
    @Order(24)
    @DisplayName("3.5 意图路由 - 获取对应 Agent")
    void testIntentRouter_GetAgentForIntent() {
        // 测试各种意图对应的 Agent
        String agentForCreate = intentRouter.getAgentForIntent(Intent.CREATE_SURVEY);
        String agentForAnalyze = intentRouter.getAgentForIntent(Intent.ANALYZE_DATA);
        String agentForLogs = intentRouter.getAgentForIntent(Intent.QUERY_LOGS);
        String agentForGeneral = intentRouter.getAgentForIntent(Intent.GENERAL_CHAT);

        System.out.println("✅ 意图路由映射:");
        System.out.println("   - CREATE_SURVEY -> " + agentForCreate);
        System.out.println("   - ANALYZE_DATA -> " + agentForAnalyze);
        System.out.println("   - QUERY_LOGS -> " + agentForLogs);
        System.out.println("   - GENERAL_CHAT -> " + agentForGeneral);

        assertNotNull(agentForGeneral, "通用对话应该有对应的 Agent");
    }

    // ==================== 记忆系统测试 ====================

    @Test
    @Order(30)
    @DisplayName("4.1 短期记忆 - 消息存储")
    void testShortTermMemory_StoreMessages() {
        String conversationId = "test-conv-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        // 添加消息
        memory.addMessage(conversationId, UserMessage.from("测试消息1"));
        memory.addMessage(conversationId, UserMessage.from("测试消息2"));
        memory.addMessage(conversationId, UserMessage.from("测试消息3"));

        // 获取消息
        List<ChatMessage> messages = memory.getMessages(conversationId);

        assertNotNull(messages, "消息列表不能为空");
        assertEquals(3, messages.size(), "应该有3条消息");

        System.out.println("✅ 短期记忆存储成功");
        System.out.println("   - 消息数量: " + messages.size());
    }

    @Test
    @Order(31)
    @DisplayName("4.2 短期记忆 - 上下文存储")
    void testShortTermMemory_Context() {
        String conversationId = "test-conv-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        // 存储上下文
        memory.setContext(conversationId, "surveyId", "survey-123");
        memory.setContext(conversationId, "step", "creating");

        // 获取上下文
        String surveyId = memory.getContext(conversationId, "surveyId");
        String step = memory.getContext(conversationId, "step");

        assertEquals("survey-123", surveyId, "应该能获取 surveyId");
        assertEquals("creating", step, "应该能获取 step");

        // 获取所有上下文
        Map<String, Object> allContext = memory.getAllContext(conversationId);
        assertTrue(allContext.size() >= 2, "应该有至少2个上下文项");

        System.out.println("✅ 上下文存储成功");
        System.out.println("   - 上下文项: " + allContext.keySet());
    }

    @Test
    @Order(32)
    @DisplayName("4.3 短期记忆 - 滑动窗口")
    void testShortTermMemory_SlidingWindow() {
        String conversationId = "test-conv-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        int windowSize = agentProperties.getMemory().getShortTerm().getWindowSize();

        // 添加超过窗口大小的消息
        for (int i = 0; i < windowSize + 5; i++) {
            memory.addMessage(conversationId, UserMessage.from("消息 " + i));
        }

        // 获取消息
        List<ChatMessage> messages = memory.getMessages(conversationId);

        // 验证窗口大小限制
        assertTrue(messages.size() <= windowSize, "消息数量应该不超过窗口大小");

        System.out.println("✅ 滑动窗口验证成功");
        System.out.println("   - 窗口大小: " + windowSize);
        System.out.println("   - 实际消息数: " + messages.size());
    }

    @Test
    @Order(33)
    @DisplayName("4.4 记忆存在性检查")
    void testMemoryExists() {
        String existingConv = "existing-conv-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String nonExistingConv = "non-existing-conv-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);

        // 添加消息到存在的会话
        memory.addMessage(existingConv, UserMessage.from("测试"));

        assertTrue(memory.exists(existingConv), "存在的会话应该返回 true");
        assertFalse(memory.exists(nonExistingConv), "不存在的会话应该返回 false");

        System.out.println("✅ 记忆存在性检查通过");
    }

    // ==================== Agent 执行测试 ====================

    @Test
    @Order(40)
    @DisplayName("5.1 Agent 执行 - 通用对话")
    void testAgentExecute_GeneralChat() {
        AgentRequest request = AgentRequest.of(testConversationId, TEST_USER_ID, "你好，请介绍一下你自己");

        AgentResponse response = orchestrator.execute(request);

        assertNotNull(response, "响应不能为空");
        assertNotNull(response.getContent(), "响应内容不能为空");

        System.out.println("✅ Agent 执行成功");
        System.out.println("   - 会话ID: " + response.getConversationId());
        System.out.println("   - 响应长度: " + response.getContent().length() + " 字符");
        System.out.println("   - 响应预览: " +
            (response.getContent().length() > 100
                ? response.getContent().substring(0, 100) + "..."
                : response.getContent()));
    }

    @Test
    @Order(41)
    @DisplayName("5.2 Agent 执行 - 带意图识别")
    void testAgentExecute_WithIntentRecognition() {
        AgentRequest request = AgentRequest.of(testConversationId, TEST_USER_ID, "我想创建一个员工满意度调查问卷");

        AgentResponse response = orchestrator.execute(request);

        assertNotNull(response);
        assertNotNull(response.getContent());

        System.out.println("✅ 带意图识别的执行成功");
        System.out.println("   - 响应: " +
            (response.getContent().length() > 150
                ? response.getContent().substring(0, 150) + "..."
                : response.getContent()));
    }

    @Test
    @Order(42)
    @DisplayName("5.3 Agent 执行 - 多轮对话")
    void testAgentExecute_MultiTurn() {
        String conversationId = "multi-turn-" + UUID.randomUUID().toString().replace("-", "").substring(0, 18);

        // 第一轮
        AgentRequest request1 = AgentRequest.of(conversationId, TEST_USER_ID, "你好");
        AgentResponse response1 = orchestrator.execute(request1);
        assertNotNull(response1.getContent());

        // 第二轮 - 应该记住上下文
        AgentRequest request2 = AgentRequest.of(conversationId, TEST_USER_ID, "我刚才说了什么？");
        AgentResponse response2 = orchestrator.execute(request2);
        assertNotNull(response2.getContent());

        System.out.println("✅ 多轮对话测试成功");
        System.out.println("   - 第一轮响应: " + response1.getContent().substring(0, Math.min(50, response1.getContent().length())) + "...");
        System.out.println("   - 第二轮响应: " + response2.getContent().substring(0, Math.min(50, response2.getContent().length())) + "...");
    }

    // ==================== 编排器测试 ====================

    @Test
    @Order(50)
    @DisplayName("6.1 编排器 - 获取可用 Agent")
    void testOrchestrator_GetAvailableAgents() {
        List<Agent> availableAgents = orchestrator.getAvailableAgents();

        assertNotNull(availableAgents);
        assertFalse(availableAgents.isEmpty());

        System.out.println("✅ 可用 Agent 列表:");
        availableAgents.forEach(agent ->
            System.out.println("   - " + agent.getName() + " (" + agent.getType() + ")")
        );
    }

    @Test
    @Order(51)
    @DisplayName("6.2 编排器 - 获取指定 Agent")
    void testOrchestrator_GetAgent() {
        Agent routerAgent = orchestrator.getAgent("routerAgent");

        assertNotNull(routerAgent, "RouterAgent 应该存在");
        assertEquals("RouterAgent", routerAgent.getName());

        System.out.println("✅ 获取指定 Agent 成功: " + routerAgent.getName());
    }

    // ==================== 协调器测试 ====================

    @Test
    @Order(60)
    @DisplayName("7.1 协调器 - 单 Agent 执行")
    void testCoordinator_SingleAgent() {
        AgentRequest request = AgentRequest.of(testConversationId, TEST_USER_ID, "简单问候测试");

        AgentResponse response = coordinator.coordinate(request);

        assertNotNull(response);
        assertNotNull(response.getContent());

        System.out.println("✅ 协调器单 Agent 执行成功");
    }

    // ==================== LLM 连接测试 ====================

    @Test
    @Order(100)
    @DisplayName("8.1 LLM 连接测试 - 简单对话")
    void testLlmConnection() {
        String response = chatModel.chat("你好，请用一句话回复");

        assertNotNull(response, "LLM 响应不能为空");
        assertFalse(response.isEmpty(), "LLM 响应不能为空字符串");

        System.out.println("✅ LLM 连接成功");
        System.out.println("   - 响应: " + response);
    }

    @Test
    @Order(101)
    @DisplayName("8.2 LLM 连接测试 - 问卷创建提示")
    void testLlmConnection_SurveyPrompt() {
        String prompt = "请帮我设计一个简单的客户满意度调查问卷，包含3个问题";
        String response = chatModel.chat(prompt);

        assertNotNull(response);
        assertTrue(response.length() > 50, "问卷设计响应应该较长");

        System.out.println("✅ LLM 问卷创建测试成功");
        System.out.println("   - 响应长度: " + response.length() + " 字符");
    }

    // ==================== 清理测试 ====================

    @Test
    @Order(200)
    @DisplayName("清理测试数据")
    void cleanup() {
        // 清理测试会话
        if (testConversationId != null) {
            memory.clear(testConversationId);
        }

        System.out.println("✅ 测试数据清理完成");
    }
}
