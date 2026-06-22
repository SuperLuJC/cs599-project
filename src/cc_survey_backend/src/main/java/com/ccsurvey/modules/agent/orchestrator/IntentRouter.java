package com.ccsurvey.modules.agent.orchestrator;

import com.ccsurvey.modules.agent.config.AgentProperties;
import com.ccsurvey.modules.agent.intent.Intent;
import com.ccsurvey.modules.agent.intent.IntentClassifier;
import com.ccsurvey.modules.agent.intent.IntentResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 意图路由器
 * 根据用户消息识别意图并路由到对应的 Agent
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IntentRouter {

    private final List<IntentClassifier> classifiers;
    private final AgentProperties agentProperties;

    /**
     * 意图到 Agent 的映射
     */
    private static final Map<Intent, String> INTENT_AGENT_MAP = Map.of(
            Intent.CREATE_SURVEY, "surveyAgent",
            Intent.UPDATE_SURVEY, "surveyAgent",
            Intent.SAVE_DRAFT, "surveyAgent",
            Intent.ANALYZE_DATA, "dataAnalysisAgent",
            Intent.QUERY_LOGS, "logAnalysisAgent",
            Intent.NATURAL_QUERY, "naturalQueryAgent",
            Intent.GENERAL_CHAT, "routerAgent",
            Intent.MULTI_AGENT_TASK, "coordinatorAgent",
            Intent.UNKNOWN, "routerAgent"
    );

    /**
     * 路由用户消息
     *
     * @param message 用户消息
     * @param context 上下文信息
     * @return 意图识别结果
     */
    public IntentResult route(String message, Object context) {
        log.debug("Routing message: {}", message);

        String classifierType = agentProperties.getIntent().getClassifier();
        float confidenceThreshold = agentProperties.getIntent().getConfidenceThreshold();

        IntentResult result;

        switch (classifierType.toLowerCase()) {
            case "rule":
                // 只使用规则匹配
                result = classifyWithRule(message, context);
                break;

            case "llm":
                // 只使用 LLM 分类
                result = classifyWithLlm(message, context);
                break;

            case "hybrid":
                // 混合策略：先规则匹配，置信度低时再用 LLM
                result = classifyHybrid(message, context, confidenceThreshold);
                break;

            default:
                log.warn("Unknown classifier type: {}, using hybrid", classifierType);
                result = classifyHybrid(message, context, confidenceThreshold);
        }

        log.info("Intent routing result: intent={}, confidence={}, multiAgent={}",
                result.getPrimaryIntent(), result.getConfidence(), result.isMultiAgent());

        return result;
    }

    /**
     * 获取意图对应的 Agent 名称
     */
    public String getAgentForIntent(Intent intent) {
        return INTENT_AGENT_MAP.getOrDefault(intent, "routerAgent");
    }

    /**
     * 获取多个意图对应的 Agent 名称列表
     */
    public List<String> getAgentsForIntents(List<Intent> intents) {
        if (intents == null || intents.isEmpty()) {
            return List.of("routerAgent");
        }

        return intents.stream()
                .map(INTENT_AGENT_MAP::get)
                .distinct()
                .toList();
    }

    /**
     * 规则匹配分类
     */
    private IntentResult classifyWithRule(String message, Object context) {
        IntentClassifier ruleClassifier = findClassifier("rule-based");
        if (ruleClassifier != null) {
            return ruleClassifier.classify(message, context);
        }
        return IntentResult.unknown();
    }

    /**
     * LLM 分类
     */
    private IntentResult classifyWithLlm(String message, Object context) {
        IntentClassifier llmClassifier = findClassifier("llm");
        if (llmClassifier != null) {
            return llmClassifier.classify(message, context);
        }
        return IntentResult.unknown();
    }

    /**
     * 混合分类策略
     */
    private IntentResult classifyHybrid(String message, Object context, float threshold) {
        // 1. 先用规则匹配（快速、低成本）
        IntentClassifier ruleClassifier = findClassifier("rule-based");
        IntentResult ruleResult = null;
        if (ruleClassifier != null) {
            ruleResult = ruleClassifier.classify(message, context);

            // 如果置信度足够高，直接返回
            if (ruleResult.getConfidence() >= threshold) {
                log.debug("Rule-based classification sufficient: confidence={}", ruleResult.getConfidence());
                return ruleResult;
            }
        }

        // 2. 置信度低时，使用 LLM 分类（更准确）
        IntentClassifier llmClassifier = findClassifier("llm");
        if (llmClassifier != null) {
            IntentResult llmResult = llmClassifier.classify(message, context);
            log.debug("LLM classification result: intent={}, confidence={}",
                    llmResult.getPrimaryIntent(), llmResult.getConfidence());

            // 如果 LLM 分类成功（不是 UNKNOWN），返回 LLM 结果
            if (llmResult.getPrimaryIntent() != Intent.UNKNOWN && llmResult.getConfidence() > 0) {
                return llmResult;
            }
            // LLM 分类失败，继续使用规则结果
            log.debug("LLM classification failed, falling back to rule-based result");
        }

        // 3. 如果 LLM 失败，返回规则匹配结果
        if (ruleResult != null && ruleResult.getPrimaryIntent() != Intent.UNKNOWN) {
            return ruleResult;
        }

        // 4. 默认返回 GENERAL_CHAT
        return IntentResult.simple(Intent.GENERAL_CHAT, 0.5f);
    }

    /**
     * 查找指定名称的分类器
     */
    private IntentClassifier findClassifier(String name) {
        return classifiers.stream()
                .filter(c -> c.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }
}