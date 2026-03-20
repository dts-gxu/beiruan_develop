package com.ruoyi.web.controller.ai;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import dev.langchain4j.http.client.HttpClientBuilder;
import dev.langchain4j.http.client.jdk.JdkHttpClientBuilder;
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
     * 自定义HttpClientBuilder，强制HTTP/1.1避免HTTP/2 RST_STREAM错误
     * 同时设置connectTimeout和readTimeout防止chunked transfer encoding连接被重置
     */
    private HttpClientBuilder createHttp11ClientBuilder()
    {
        HttpClient.Builder jdkBuilder = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(timeout));
        return new JdkHttpClientBuilder()
                .httpClientBuilder(jdkBuilder)
                .connectTimeout(Duration.ofSeconds(timeout))
                .readTimeout(Duration.ofSeconds(timeout));
    }

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
                .httpClientBuilder(createHttp11ClientBuilder())
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
                .httpClientBuilder(createHttp11ClientBuilder())
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
