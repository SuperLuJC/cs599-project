package com.ccsurvey.modules.agent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Agent 模块配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "agent")
public class AgentProperties {

    /**
     * LLM 配置
     */
    private LlmConfig llm = new LlmConfig();

    /**
     * 记忆配置
     */
    private MemoryConfig memory = new MemoryConfig();

    /**
     * 意图识别配置
     */
    private IntentConfig intent = new IntentConfig();

    /**
     * 编排配置
     */
    private OrchestrationConfig orchestration = new OrchestrationConfig();

    @Data
    public static class LlmConfig {
        /**
         * 当前使用的提供商: xfyun, deepseek, aliyun
         */
        private String provider = "xfyun";

        private ProviderConfig xfyun = new ProviderConfig();
        private ProviderConfig deepseek = new ProviderConfig();
        private ProviderConfig aliyun = new ProviderConfig();
    }

    @Data
    public static class ProviderConfig {
        private String baseUrl;
        private String apiKey;
        private String model;
        private Double temperature = 0.7;
        private Integer maxTokens = 4096;
    }

    @Data
    public static class MemoryConfig {
        private ShortTermConfig shortTerm = new ShortTermConfig();
        private LongTermConfig longTerm = new LongTermConfig();
    }

    @Data
    public static class ShortTermConfig {
        private Boolean enabled = true;
        private Integer windowSize = 20;
        private Integer ttlMinutes = 30;
    }

    @Data
    public static class LongTermConfig {
        private Boolean enabled = true;
        private Integer summarizeThreshold = 50;
    }

    @Data
    public static class IntentConfig {
        /**
         * 分类器类型: rule, llm, hybrid
         */
        private String classifier = "hybrid";
        private Float confidenceThreshold = 0.7f;
    }

    @Data
    public static class OrchestrationConfig {
        private Integer maxIterations = 5;
        private Integer timeoutSeconds = 60;
        private Boolean parallelExecution = true;
    }
}
