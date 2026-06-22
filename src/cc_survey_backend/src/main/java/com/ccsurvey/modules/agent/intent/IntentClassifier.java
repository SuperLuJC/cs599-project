package com.ccsurvey.modules.agent.intent;

/**
 * 意图分类器接口
 */
public interface IntentClassifier {

    /**
     * 分类用户消息的意图
     *
     * @param message     用户消息
     * @param context     上下文信息（可选）
     * @return 意图识别结果
     */
    IntentResult classify(String message, Object context);

    /**
     * 获取分类器名称
     */
    String getName();

    /**
     * 获取分类器优先级（数值越小优先级越高）
     */
    default int getPriority() {
        return 100;
    }
}