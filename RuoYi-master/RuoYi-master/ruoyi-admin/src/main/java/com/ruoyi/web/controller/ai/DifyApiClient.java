package com.ruoyi.web.controller.ai;

import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.system.domain.AiDifyApp;
import com.ruoyi.system.domain.AiDifyConfig;
import com.ruoyi.system.mapper.AiDifyAppMapper;
import com.ruoyi.system.mapper.AiDifyConfigMapper;

/**
 * Dify API统一HTTP客户端
 * 
 * 职责：
 * 1. 从数据库动态读取API配置（非hardcode在yml）
 * 2. 封装所有Dify REST API调用
 * 3. 统一异常处理和日志
 * 
 * @author ruoyi
 */
@Service
public class DifyApiClient
{
    private static final Logger log = LoggerFactory.getLogger(DifyApiClient.class);

    @Autowired
    private AiDifyAppMapper aiDifyAppMapper;

    @Autowired
    private AiDifyConfigMapper aiDifyConfigMapper;

    private final RestTemplate restTemplate = new RestTemplate();

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 发送Chat消息（POST /v1/chat-messages）
     *
     * @param appId 应用ID（数据库中的ai_dify_app.app_id）
     * @param query 用户消息
     * @param user 用户标识
     * @param conversationId Dify会话ID（首次为空）
     * @return 包含answer、conversationId、messageId的Map
     */
    public Map<String, String> chatMessage(Long appId, String query, String user, String conversationId)
    {
        AppConfig cfg = resolveAppConfig(appId);
        String url = cfg.baseUrl + "/chat-messages";

        Map<String, Object> body = new HashMap<>();
        body.put("inputs", new HashMap<>());
        body.put("query", query);
        body.put("response_mode", "blocking");
        body.put("user", user != null ? user : "ruoyi-system");
        if (conversationId != null && !conversationId.isEmpty())
        {
            body.put("conversation_id", conversationId);
        }

        try
        {
            String requestJson = objectMapper.writeValueAsString(body);
            log.info("DifyApiClient.chatMessage: appId={}, query_length={}", appId, query.length());

            HttpEntity<String> request = new HttpEntity<>(requestJson, buildHeaders(cfg.apiKey));
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            Map<String, String> result = new HashMap<>();
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null)
            {
                JsonNode root = objectMapper.readTree(response.getBody());
                result.put("answer", root.path("answer").asText(""));
                result.put("conversationId", root.path("conversation_id").asText(""));
                result.put("messageId", root.path("message_id").asText(""));
                // 提取token用量
                JsonNode metadata = root.path("metadata");
                JsonNode usage = metadata.path("usage");
                result.put("totalTokens", String.valueOf(usage.path("total_tokens").asInt(0)));
                result.put("model", usage.path("model").asText(""));
                log.info("DifyApiClient.chatMessage成功: answer_length={}", result.get("answer").length());
            }
            return result;
        }
        catch (Exception e)
        {
            log.error("DifyApiClient.chatMessage失败: appId={}", appId, e);
            throw new RuntimeException("Dify Chat调用失败: " + e.getMessage(), e);
        }
    }

    /**
     * 运行工作流（POST /v1/workflows/run）
     *
     * @param appId 应用ID
     * @param inputs 工作流输入参数
     * @param user 用户标识
     * @return 工作流输出结果
     */
    public String runWorkflow(Long appId, Map<String, Object> inputs, String user)
    {
        AppConfig cfg = resolveAppConfig(appId);
        String url = cfg.baseUrl + "/workflows/run";

        Map<String, Object> body = new HashMap<>();
        body.put("inputs", inputs);
        body.put("response_mode", "blocking");
        body.put("user", user != null ? user : "ruoyi-system");

        try
        {
            String requestJson = objectMapper.writeValueAsString(body);
            log.info("DifyApiClient.runWorkflow: appId={}, inputs_keys={}", appId, inputs.keySet());

            HttpEntity<String> request = new HttpEntity<>(requestJson, buildHeaders(cfg.apiKey));
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null)
            {
                JsonNode root = objectMapper.readTree(response.getBody());
                String status = root.path("data").path("status").asText("");
                if ("succeeded".equals(status))
                {
                    JsonNode outputs = root.path("data").path("outputs");
                    if (outputs.has("result"))
                    {
                        return outputs.get("result").asText();
                    }
                    return outputs.toString();
                }
                else
                {
                    String error = root.path("data").path("error").asText("未知错误");
                    throw new RuntimeException("工作流执行失败: " + error);
                }
            }
            throw new RuntimeException("Dify API响应异常");
        }
        catch (Exception e)
        {
            log.error("DifyApiClient.runWorkflow失败: appId={}", appId, e);
            throw new RuntimeException("Dify工作流调用失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取应用信息（GET /v1/info）— 用于连接测试
     *
     * @param apiKey 应用API密钥
     * @param baseUrl Dify API地址
     * @return 应用名称，若失败则抛异常
     */
    public String getAppInfo(String apiKey, String baseUrl)
    {
        String url = baseUrl + "/info";
        try
        {
            HttpEntity<String> request = new HttpEntity<>(buildHeaders(apiKey));
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, request, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null)
            {
                JsonNode root = objectMapper.readTree(response.getBody());
                return root.path("name").asText("Dify应用");
            }
            throw new RuntimeException("响应异常: " + response.getStatusCode());
        }
        catch (Exception e)
        {
            log.error("DifyApiClient.getAppInfo失败: baseUrl={}", baseUrl, e);
            throw new RuntimeException("连接测试失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取应用参数（GET /v1/parameters）— 用于验证密钥
     *
     * @param apiKey 应用API密钥
     * @param baseUrl Dify API地址
     * @return 参数JSON字符串
     */
    public String getAppParameters(String apiKey, String baseUrl)
    {
        String url = baseUrl + "/parameters";
        try
        {
            HttpEntity<String> request = new HttpEntity<>(buildHeaders(apiKey));
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, request, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null)
            {
                return response.getBody();
            }
            throw new RuntimeException("响应异常: " + response.getStatusCode());
        }
        catch (Exception e)
        {
            log.error("DifyApiClient.getAppParameters失败: baseUrl={}", baseUrl, e);
            throw new RuntimeException("密钥验证失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取Dify会话列表（GET /v1/conversations）
     */
    public String getConversations(Long appId, String user, int limit)
    {
        AppConfig cfg = resolveAppConfig(appId);
        String url = cfg.baseUrl + "/conversations?user=" + user + "&limit=" + limit;
        try
        {
            HttpEntity<String> request = new HttpEntity<>(buildHeaders(cfg.apiKey));
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, request, String.class);
            return response.getBody();
        }
        catch (Exception e)
        {
            log.error("DifyApiClient.getConversations失败", e);
            throw new RuntimeException("获取会话列表失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取Dify消息列表（GET /v1/messages）
     */
    public String getMessages(Long appId, String conversationId, String user, int limit)
    {
        AppConfig cfg = resolveAppConfig(appId);
        String url = cfg.baseUrl + "/messages?conversation_id=" + conversationId
                + "&user=" + user + "&limit=" + limit;
        try
        {
            HttpEntity<String> request = new HttpEntity<>(buildHeaders(cfg.apiKey));
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, request, String.class);
            return response.getBody();
        }
        catch (Exception e)
        {
            log.error("DifyApiClient.getMessages失败", e);
            throw new RuntimeException("获取消息列表失败: " + e.getMessage(), e);
        }
    }

    /**
     * 根据appId从数据库解析出baseUrl和apiKey
     */
    private AppConfig resolveAppConfig(Long appId)
    {
        AiDifyApp app = aiDifyAppMapper.selectAiDifyAppByAppId(appId);
        if (app == null)
        {
            throw new RuntimeException("Dify应用不存在: appId=" + appId);
        }
        if (!"0".equals(app.getStatus()))
        {
            throw new RuntimeException("Dify应用已停用: " + app.getAppName());
        }

        AiDifyConfig config = aiDifyConfigMapper.selectAiDifyConfigByConfigId(app.getConfigId());
        if (config == null)
        {
            throw new RuntimeException("Dify配置不存在: configId=" + app.getConfigId());
        }

        AppConfig cfg = new AppConfig();
        cfg.apiKey = app.getAppApiKey();
        cfg.baseUrl = config.getBaseUrl();
        cfg.appName = app.getAppName();
        return cfg;
    }

    /**
     * 构建HTTP请求头
     */
    private HttpHeaders buildHeaders(String apiKey)
    {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);
        return headers;
    }

    /**
     * 内部配置封装
     */
    private static class AppConfig
    {
        String apiKey;
        String baseUrl;
        String appName;
    }
}
