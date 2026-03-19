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
     * 发送Chat消息
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
     * 运行工作流
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
     * 获取应用信息（用于连接测试）
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
     * 获取应用参数（用于验证密钥）
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
