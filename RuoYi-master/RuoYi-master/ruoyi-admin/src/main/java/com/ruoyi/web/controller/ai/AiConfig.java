package com.ruoyi.web.controller.ai;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;

/**
 * AI大模型配置类
 * 
 * 支持DeepSeek/OpenAI/Qwen等OpenAI兼容API
 * 
 * @author ruoyi
 */
@Configuration
public class AiConfig
{
    @Value("${ai.apiKey}")
    private String apiKey;

    @Value("${ai.baseUrl}")
    private String baseUrl;

    @Value("${ai.modelName}")
    private String modelName;

    @Value("${ai.maxTokens}")
    private int maxTokens;

    @Value("${ai.temperature}")
    private double temperature;

    @Value("${ai.timeout}")
    private int timeout;

    /**
     * 同步聊天模型（等待完整响应）
     */
    @Bean
    public ChatLanguageModel chatLanguageModel()
    {
        return OpenAiChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(modelName)
                .maxTokens(maxTokens)
                .temperature(temperature)
                .timeout(Duration.ofSeconds(timeout))
                .build();
    }

    /**
     * 流式聊天模型（SSE逐字输出）
     */
    @Bean
    public StreamingChatLanguageModel streamingChatLanguageModel()
    {
        return OpenAiStreamingChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(modelName)
                .maxTokens(maxTokens)
                .temperature(temperature)
                .timeout(Duration.ofSeconds(timeout))
                .build();
    }

    public String getApiKey()
    {
        return apiKey;
    }

    public String getBaseUrl()
    {
        return baseUrl;
    }

    public String getModelName()
    {
        return modelName;
    }

    public int getMaxTokens()
    {
        return maxTokens;
    }

    public double getTemperature()
    {
        return temperature;
    }

    public int getTimeout()
    {
        return timeout;
    }
}
