package com.ccsurvey.modules.agent.config;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.Duration;

/**
 * LangChain4j 配置类
 * 配置 ChatModel 和 StreamingChatModel Bean
 *
 * 所有提供商都通过 OpenAI 兼容接口访问：
 * - Xfyun: OpenAI 兼容 API
 * - DeepSeek: OpenAI 兼容 API
 * - Aliyun: OpenAI 兼容 API (DashScope)
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class LangChain4jConfig {

    private final AgentProperties agentProperties;

    /**
     * 讯飞云 ChatModel (通过 OpenAI 兼容接口)
     */
    @Bean
    @Primary
    @ConditionalOnProperty(name = "agent.llm.provider", havingValue = "xfyun")
    public ChatModel xfyunChatModel() {
        AgentProperties.ProviderConfig config = agentProperties.getLlm().getXfyun();
        log.info("Initializing Xfyun ChatModel with model: {}", config.getModel());

        return OpenAiChatModel.builder()
                .baseUrl(config.getBaseUrl())
                .apiKey(config.getApiKey())
                .modelName(config.getModel())
                .temperature(config.getTemperature())
                .maxTokens(config.getMaxTokens())
                .timeout(Duration.ofSeconds(60))
                .logRequests(true)
                .logResponses(true)
                .build();
    }

    /**
     * 讯飞云 StreamingChatModel
     */
    @Bean
    @Primary
    @ConditionalOnProperty(name = "agent.llm.provider", havingValue = "xfyun")
    public StreamingChatModel xfyunStreamingChatModel() {
        AgentProperties.ProviderConfig config = agentProperties.getLlm().getXfyun();
        log.info("Initializing Xfyun StreamingChatModel with model: {}", config.getModel());

        return OpenAiStreamingChatModel.builder()
                .baseUrl(config.getBaseUrl())
                .apiKey(config.getApiKey())
                .modelName(config.getModel())
                .temperature(config.getTemperature())
                .maxTokens(config.getMaxTokens())
                .timeout(Duration.ofSeconds(60))
                .logRequests(true)
                .logResponses(true)
                .build();
    }

    /**
     * DeepSeek ChatModel (通过 OpenAI 兼容接口)
     */
    @Bean
    @Primary
    @ConditionalOnProperty(name = "agent.llm.provider", havingValue = "deepseek")
    public ChatModel deepSeekChatModel() {
        AgentProperties.ProviderConfig config = agentProperties.getLlm().getDeepseek();
        log.info("Initializing DeepSeek ChatModel with model: {}", config.getModel());

        return OpenAiChatModel.builder()
                .baseUrl(config.getBaseUrl())
                .apiKey(config.getApiKey())
                .modelName(config.getModel())
                .temperature(config.getTemperature())
                .maxTokens(config.getMaxTokens())
                .timeout(Duration.ofSeconds(60))
                .logRequests(true)
                .logResponses(true)
                .build();
    }

    /**
     * DeepSeek StreamingChatModel
     */
    @Bean
    @Primary
    @ConditionalOnProperty(name = "agent.llm.provider", havingValue = "deepseek")
    public StreamingChatModel deepSeekStreamingChatModel() {
        AgentProperties.ProviderConfig config = agentProperties.getLlm().getDeepseek();
        log.info("Initializing DeepSeek StreamingChatModel with model: {}", config.getModel());

        return OpenAiStreamingChatModel.builder()
                .baseUrl(config.getBaseUrl())
                .apiKey(config.getApiKey())
                .modelName(config.getModel())
                .temperature(config.getTemperature())
                .maxTokens(config.getMaxTokens())
                .timeout(Duration.ofSeconds(60))
                .logRequests(true)
                .logResponses(true)
                .build();
    }

    /**
     * 阿里云百炼 ChatModel (通过 OpenAI 兼容接口)
     * DashScope 支持 OpenAI 兼容模式
     */
    @Bean
    @Primary
    @ConditionalOnProperty(name = "agent.llm.provider", havingValue = "aliyun")
    public ChatModel aliyunChatModel() {
        AgentProperties.ProviderConfig config = agentProperties.getLlm().getAliyun();
        log.info("Initializing Aliyun DashScope ChatModel with model: {}", config.getModel());

        // 阿里云 DashScope OpenAI 兼容接口
        String baseUrl = config.getBaseUrl();
        if (baseUrl == null || baseUrl.isEmpty()) {
            baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1";
        }

        return OpenAiChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(config.getApiKey())
                .modelName(config.getModel())
                .temperature(config.getTemperature())
                .maxTokens(config.getMaxTokens())
                .timeout(Duration.ofSeconds(60))
                .logRequests(true)
                .logResponses(true)
                .build();
    }

    /**
     * 阿里云百炼 StreamingChatModel
     */
    @Bean
    @Primary
    @ConditionalOnProperty(name = "agent.llm.provider", havingValue = "aliyun")
    public StreamingChatModel aliyunStreamingChatModel() {
        AgentProperties.ProviderConfig config = agentProperties.getLlm().getAliyun();
        log.info("Initializing Aliyun DashScope StreamingChatModel with model: {}", config.getModel());

        // 阿里云 DashScope OpenAI 兼容接口
        String baseUrl = config.getBaseUrl();
        if (baseUrl == null || baseUrl.isEmpty()) {
            baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1";
        }

        return OpenAiStreamingChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(config.getApiKey())
                .modelName(config.getModel())
                .temperature(config.getTemperature())
                .maxTokens(config.getMaxTokens())
                .timeout(Duration.ofSeconds(60))
                .logRequests(true)
                .logResponses(true)
                .build();
    }
}
