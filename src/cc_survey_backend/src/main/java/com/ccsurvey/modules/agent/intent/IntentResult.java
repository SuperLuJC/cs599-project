package com.ccsurvey.modules.agent.intent;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 意图识别结果
 */
@Data
public class IntentResult {

    /**
     * 主要意图
     */
    private Intent primaryIntent;

    /**
     * 次要意图列表（用于多意图场景）
     */
    private List<Intent> secondaryIntents;

    /**
     * 提取的实体信息
     * 例如：问卷主题、数据范围、时间范围等
     */
    private Map<String, Object> entities;

    /**
     * 置信度 (0-1)
     */
    private float confidence;

    /**
     * 推理过程说明
     */
    private String reasoning;

    /**
     * 是否需要多 Agent 协作
     */
    private boolean multiAgent;

    /**
     * 创建简单意图结果
     */
    public static IntentResult simple(Intent intent, float confidence) {
        IntentResult result = new IntentResult();
        result.setPrimaryIntent(intent);
        result.setConfidence(confidence);
        result.setMultiAgent(false);
        return result;
    }

    /**
     * 创建带实体的意图结果
     */
    public static IntentResult withEntities(Intent intent, float confidence, Map<String, Object> entities) {
        IntentResult result = new IntentResult();
        result.setPrimaryIntent(intent);
        result.setConfidence(confidence);
        result.setEntities(entities);
        result.setMultiAgent(false);
        return result;
    }

    /**
     * 创建多意图结果
     */
    public static IntentResult multi(List<Intent> intents, float confidence) {
        IntentResult result = new IntentResult();
        if (intents != null && !intents.isEmpty()) {
            result.setPrimaryIntent(intents.get(0));
            result.setSecondaryIntents(intents.size() > 1 ? intents.subList(1, intents.size()) : null);
        }
        result.setConfidence(confidence);
        result.setMultiAgent(true);
        return result;
    }

    /**
     * 创建未知意图结果
     */
    public static IntentResult unknown() {
        IntentResult result = new IntentResult();
        result.setPrimaryIntent(Intent.UNKNOWN);
        result.setConfidence(0);
        result.setMultiAgent(false);
        return result;
    }
}