package com.ccsurvey.modules.agent.intent;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * LLM 意图分类器
 * 使用大语言模型进行意图识别
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LlmIntentClassifier implements IntentClassifier {

    private final ChatModel chatModel;

    private static final String INTENT_CLASSIFICATION_PROMPT = """
            你是一个意图识别助手。分析用户消息，识别用户意图并返回 JSON 格式结果。

            可选意图类型：
            - CREATE_SURVEY: 创建问卷、新建调查、生成表单
            - UPDATE_SURVEY: 修改问卷、更新调查、编辑表单
            - SAVE_DRAFT: 保存草稿、存草稿、确认保存问卷
            - ANALYZE_DATA: 数据分析、统计查询、查看提交数据
            - QUERY_LOGS: 日志查询、操作记录、活动历史
            - GENERAL_CHAT: 通用对话、问候、闲聊
            - MULTI_AGENT_TASK: 复合任务（需要多个操作）

            用户消息：{{message}}

            请返回 JSON 格式结果：
            {
                "intent": "<意图类型>",
                "confidence": <0.0-1.0>,
                "entities": {
                    "survey_topic": "<问卷主题，如果涉及>",
                    "time_range": "<时间范围，如果涉及>",
                    "data_range": "<数据范围，如果涉及>"
                },
                "reasoning": "<简短解释为什么是这个意图>"
            }

            只返回 JSON，不要其他内容。
            """;

    @Override
    public IntentResult classify(String message, Object context) {
        if (message == null || message.trim().isEmpty()) {
            return IntentResult.unknown();
        }

        try {
            // 构建提示词
            String prompt = INTENT_CLASSIFICATION_PROMPT.replace("{{message}}", message);

            // 使用简单的字符串聊天方法
            String response = chatModel.chat(
                    "你是一个专业的意图识别助手，只返回 JSON 格式结果。\n\n用户消息：" + message
            );

            // 解析结果
            return parseLlmResponse(response);

        } catch (Exception e) {
            log.error("LLM intent classification failed: {}", e.getMessage());
            return IntentResult.unknown();
        }
    }

    @Override
    public String getName() {
        return "llm";
    }

    @Override
    public int getPriority() {
        return 50;  // 中等优先级，在规则匹配之后
    }

    /**
     * 解析 LLM 响应
     */
    private IntentResult parseLlmResponse(String response) {
        try {
            // 提取 JSON
            String jsonStr = extractJson(response);
            if (jsonStr == null) {
                return IntentResult.simple(Intent.GENERAL_CHAT, 0.5f);
            }

            JSONObject json = JSON.parseObject(jsonStr);

            // 解析意图
            String intentStr = json.getString("intent");
            Intent intent = Intent.fromString(intentStr);

            // 如果意图不在已知列表中，归类为通用对话
            if (intent == Intent.UNKNOWN && intentStr != null) {
                log.debug("Unknown intent from LLM: {}, treating as GENERAL_CHAT", intentStr);
                intent = Intent.GENERAL_CHAT;
            }

            // 解析置信度
            float confidence = json.getFloatValue("confidence");

            // 解析实体
            Map<String, Object> entities = new HashMap<>();
            JSONObject entitiesJson = json.getJSONObject("entities");
            if (entitiesJson != null) {
                for (String key : entitiesJson.keySet()) {
                    entities.put(key, entitiesJson.get(key));
                }
            }

            // 解析推理过程
            String reasoning = json.getString("reasoning");

            IntentResult result = IntentResult.withEntities(intent, confidence, entities);
            result.setReasoning(reasoning);

            return result;

        } catch (Exception e) {
            log.error("Failed to parse LLM response: {}", e.getMessage());
            return IntentResult.simple(Intent.GENERAL_CHAT, 0.5f);
        }
    }

    /**
     * 从响应中提取 JSON
     */
    private String extractJson(String response) {
        if (response == null) {
            return null;
        }

        // 尝试直接解析
        String trimmed = response.trim();
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            return trimmed;
        }

        // 尝试提取 JSON 块
        int start = response.indexOf('{');
        int end = response.lastIndexOf('}');

        if (start >= 0 && end > start) {
            return response.substring(start, end + 1);
        }

        return null;
    }
}
