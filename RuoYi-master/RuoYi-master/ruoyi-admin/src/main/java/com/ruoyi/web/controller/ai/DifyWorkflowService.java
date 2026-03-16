package com.ruoyi.web.controller.ai;

import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Dify Workflow API 集成服务
 * 
 * 通过HTTP调用Dify平台的Workflow API，实现AI开发全流程编排。
 * Dify负责：工作流编排、RAG知识库检索、LLM调用、上下文管理。
 * RuoYi负责：用户交互、文件解析、结果展示、权限控制。
 * 
 * @author ruoyi
 */
@Service
public class DifyWorkflowService
{
    private static final Logger log = LoggerFactory.getLogger(DifyWorkflowService.class);

    @Value("${dify.baseUrl:http://localhost:8180/v1}")
    private String difyBaseUrl;

    @Value("${dify.apiKey:}")
    private String difyApiKey;

    @Value("${dify.enabled:false}")
    private boolean difyEnabled;

    @Value("${dify.workflowId:}")
    private String defaultWorkflowId;

    @Value("${dify.timeout:180}")
    private int timeout;

    private final RestTemplate restTemplate = new RestTemplate();

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 是否启用Dify工作流（未启用时降级为直接LangChain4j调用）
     */
    public boolean isEnabled()
    {
        return difyEnabled && difyApiKey != null && !difyApiKey.isEmpty();
    }

    /**
     * 运行Dify工作流（阻塞模式）
     *
     * @param inputs 工作流输入参数
     * @param user 用户标识
     * @return 工作流输出结果
     */
    public String runWorkflow(Map<String, Object> inputs, String user)
    {
        return runWorkflow(inputs, user, defaultWorkflowId);
    }

    /**
     * 运行指定Dify工作流（阻塞模式）
     *
     * @param inputs 工作流输入参数
     * @param user 用户标识
     * @param workflowId 工作流ID（如果为空则使用默认）
     * @return 工作流输出结果
     */
    public String runWorkflow(Map<String, Object> inputs, String user, String workflowId)
    {
        String url = difyBaseUrl + "/workflows/run";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + difyApiKey);

        Map<String, Object> body = new HashMap<>();
        body.put("inputs", inputs);
        body.put("response_mode", "blocking");
        body.put("user", user != null ? user : "ruoyi-system");

        try
        {
            String requestJson = objectMapper.writeValueAsString(body);
            log.info("Dify工作流请求: url={}, inputs_keys={}", url, inputs.keySet());

            HttpEntity<String> request = new HttpEntity<>(requestJson, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null)
            {
                JsonNode root = objectMapper.readTree(response.getBody());

                // 检查工作流执行状态
                String status = root.path("data").path("status").asText("");
                if ("succeeded".equals(status))
                {
                    // 提取outputs中的结果
                    JsonNode outputs = root.path("data").path("outputs");
                    if (outputs.has("result"))
                    {
                        String result = outputs.get("result").asText();
                        log.info("Dify工作流执行成功, 结果长度: {}", result.length());
                        return result;
                    }
                    // 如果没有result字段，返回整个outputs
                    String outputStr = outputs.toString();
                    log.info("Dify工作流执行成功, outputs: {}", outputStr);
                    return outputStr;
                }
                else
                {
                    String error = root.path("data").path("error").asText("未知错误");
                    log.error("Dify工作流执行失败: status={}, error={}", status, error);
                    throw new RuntimeException("工作流执行失败: " + error);
                }
            }
            else
            {
                log.error("Dify API响应异常: status={}", response.getStatusCode());
                throw new RuntimeException("Dify API响应异常: " + response.getStatusCode());
            }
        }
        catch (Exception e)
        {
            log.error("Dify工作流调用失败", e);
            throw new RuntimeException("Dify工作流调用失败: " + e.getMessage(), e);
        }
    }

    /**
     * 调用Dify Chat API（对话模式，适合单轮问答）
     *
     * @param query 用户问题
     * @param user 用户标识
     * @param conversationId 会话ID（首次为空，后续传入以保持上下文）
     * @return 包含回复内容和会话ID的Map
     */
    public Map<String, String> chat(String query, String user, String conversationId)
    {
        String url = difyBaseUrl + "/chat-messages";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + difyApiKey);

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
            log.info("Dify Chat请求: query_length={}", query.length());

            HttpEntity<String> request = new HttpEntity<>(requestJson, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            Map<String, String> result = new HashMap<>();
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null)
            {
                JsonNode root = objectMapper.readTree(response.getBody());
                result.put("answer", root.path("answer").asText(""));
                result.put("conversationId", root.path("conversation_id").asText(""));
                log.info("Dify Chat响应成功, answer_length={}", result.get("answer").length());
            }
            return result;
        }
        catch (Exception e)
        {
            log.error("Dify Chat调用失败", e);
            throw new RuntimeException("Dify Chat调用失败: " + e.getMessage(), e);
        }
    }

    /**
     * 运行需求分析全流程工作流
     * 
     * 将需求文档作为输入，Dify工作流内部编排：
     * 1.3 关键信息提取 → 1.4 RuoYi适配分析 → 1.5 完整性评估 → 1.6 报告生成 → 1.7 JSON生成
     *
     * @param documentContent 需求文档内容
     * @param user 用户标识
     * @return 工作流完整输出
     */
    public String runRequirementAnalysis(String documentContent, String user)
    {
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("document", documentContent);
        return runWorkflow(inputs, user);
    }

    /**
     * 运行单个步骤的工作流
     *
     * @param stepKey 步骤标识 (如 "1.3", "1.4", "1.5", "1.6", "1.7")
     * @param input 步骤输入内容
     * @param user 用户标识
     * @return 步骤输出结果
     */
    public String runStep(String stepKey, String input, String user)
    {
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("step", stepKey);
        inputs.put("input", input);
        return runWorkflow(inputs, user);
    }
}
