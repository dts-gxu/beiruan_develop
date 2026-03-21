package com.ruoyi.web.controller.ai;

import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Dify知识库API客户端
 * 参考 proj-hp-tdhpai/max-serve 的 DatasetApiService 模式
 * 对接 Dify Knowledge Base REST API
 * 
 * @author ruoyi
 */
@Service
public class DifyKnowledgeApiClient
{
    private static final Logger log = LoggerFactory.getLogger(DifyKnowledgeApiClient.class);

    private final RestTemplate restTemplate = new RestTemplate();

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ==================== 知识库操作 ====================

    /**
     * 创建空知识库
     * POST /v1/datasets
     */
    public JsonNode createDataset(String baseUrl, String apiKey, String name, String description)
    {
        String url = baseUrl + "/v1/datasets";
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("permission", "only_me");
        if (description != null && !description.isEmpty())
        {
            body.put("description", description);
        }
        return doPost(url, apiKey, body);
    }

    /**
     * 获取知识库列表
     * GET /v1/datasets?page=1&limit=20
     */
    public JsonNode listDatasets(String baseUrl, String apiKey, int page, int limit)
    {
        String url = baseUrl + "/v1/datasets?page=" + page + "&limit=" + limit;
        return doGet(url, apiKey);
    }

    /**
     * 删除知识库
     * DELETE /v1/datasets/{dataset_id}
     */
    public boolean deleteDataset(String baseUrl, String apiKey, String datasetId)
    {
        String url = baseUrl + "/v1/datasets/" + datasetId;
        return doDelete(url, apiKey);
    }

    // ==================== 文档操作 ====================

    /**
     * 通过文本创建文档
     * POST /v1/datasets/{dataset_id}/document/create_by_text
     */
    public JsonNode createDocumentByText(String baseUrl, String apiKey, String datasetId,
                                          String name, String text)
    {
        String url = baseUrl + "/v1/datasets/" + datasetId + "/document/create_by_text";
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("text", text);
        body.put("indexing_technique", "high_quality");
        Map<String, Object> processRule = new HashMap<>();
        processRule.put("mode", "automatic");
        body.put("process_rule", processRule);
        return doPost(url, apiKey, body);
    }

    /**
     * 通过文件创建文档
     * POST /v1/datasets/{dataset_id}/document/create-by-file
     */
    public JsonNode createDocumentByFile(String baseUrl, String apiKey, String datasetId,
                                          MultipartFile file)
    {
        String url = baseUrl + "/v1/datasets/" + datasetId + "/document/create-by-file";
        try
        {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            headers.set("Authorization", "Bearer " + apiKey);

            MultiValueMap<String, Object> formData = new LinkedMultiValueMap<>();

            // data参数
            String dataJson = objectMapper.writeValueAsString(Map.of(
                "indexing_technique", "high_quality",
                "process_rule", Map.of("mode", "automatic")
            ));
            formData.add("data", dataJson);

            // file参数
            ByteArrayResource fileResource = new ByteArrayResource(file.getBytes())
            {
                @Override
                public String getFilename()
                {
                    return file.getOriginalFilename();
                }
            };
            formData.add("file", fileResource);

            HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(formData, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null)
            {
                return objectMapper.readTree(response.getBody());
            }
            throw new RuntimeException("Dify API响应异常: " + response.getStatusCode());
        }
        catch (Exception e)
        {
            log.error("创建文件文档失败: datasetId={}", datasetId, e);
            throw new RuntimeException("创建文件文档失败: " + e.getMessage(), e);
        }
    }

    /**
     * 更新文档（文本方式）
     * POST /v1/datasets/{dataset_id}/documents/{document_id}/update_by_text
     */
    public JsonNode updateDocumentByText(String baseUrl, String apiKey, String datasetId,
                                          String documentId, String name, String text)
    {
        String url = baseUrl + "/v1/datasets/" + datasetId + "/documents/" + documentId + "/update_by_text";
        Map<String, Object> body = new HashMap<>();
        if (name != null)
        {
            body.put("name", name);
        }
        if (text != null)
        {
            body.put("text", text);
        }
        return doPost(url, apiKey, body);
    }

    /**
     * 获取文档列表
     * GET /v1/datasets/{dataset_id}/documents
     */
    public JsonNode listDocuments(String baseUrl, String apiKey, String datasetId, int page, int limit)
    {
        String url = baseUrl + "/v1/datasets/" + datasetId + "/documents?page=" + page + "&limit=" + limit;
        return doGet(url, apiKey);
    }

    /**
     * 删除文档
     * DELETE /v1/datasets/{dataset_id}/documents/{document_id}
     */
    public boolean deleteDocument(String baseUrl, String apiKey, String datasetId, String documentId)
    {
        String url = baseUrl + "/v1/datasets/" + datasetId + "/documents/" + documentId;
        return doDelete(url, apiKey);
    }

    /**
     * 获取文档索引状态
     * GET /v1/datasets/{dataset_id}/documents/{batch}/indexing-status
     */
    public JsonNode getIndexingStatus(String baseUrl, String apiKey, String datasetId, String batch)
    {
        String url = baseUrl + "/v1/datasets/" + datasetId + "/documents/" + batch + "/indexing-status";
        return doGet(url, apiKey);
    }

    // ==================== 知识库检索（RAG） ====================

    /**
     * 知识库检索（Dify原生RAG检索）
     * 参考 max-serve DatasetApiService.searchTest
     * POST /v1/datasets/{dataset_id}/retrieve
     */
    public JsonNode retrieveDataset(String baseUrl, String apiKey, String datasetId,
                                     String query, int topK, double scoreThreshold)
    {
        String url = baseUrl + "/v1/datasets/" + datasetId + "/retrieve";
        Map<String, Object> body = new HashMap<>();
        body.put("query", query);
        Map<String, Object> retrievalModel = new HashMap<>();
        retrievalModel.put("search_method", "semantic_search");
        retrievalModel.put("reranking_enable", false);
        retrievalModel.put("top_k", topK);
        retrievalModel.put("score_threshold_enabled", false);
        body.put("retrieval_model", retrievalModel);
        log.info("Dify检索请求: datasetId={}, query长度={}, topK={}", datasetId, query.length(), topK);
        return doPost(url, apiKey, body);
    }

    /**
     * 知识库检索（简易版 - 仅需query和topK）
     */
    public JsonNode retrieveDataset(String baseUrl, String apiKey, String datasetId, String query)
    {
        return retrieveDataset(baseUrl, apiKey, datasetId, query, 5, 0.0);
    }

    // ==================== 分段(Chunk)操作 ====================

    /**
     * 添加分段
     * POST /v1/datasets/{dataset_id}/documents/{document_id}/segments
     */
    public JsonNode addSegments(String baseUrl, String apiKey, String datasetId,
                                 String documentId, Object segments)
    {
        String url = baseUrl + "/v1/datasets/" + datasetId + "/documents/" + documentId + "/segments";
        Map<String, Object> body = new HashMap<>();
        body.put("segments", segments);
        return doPost(url, apiKey, body);
    }

    /**
     * 获取分段
     * GET /v1/datasets/{dataset_id}/documents/{document_id}/segments
     */
    public JsonNode getSegments(String baseUrl, String apiKey, String datasetId, String documentId)
    {
        String url = baseUrl + "/v1/datasets/" + datasetId + "/documents/" + documentId + "/segments";
        return doGet(url, apiKey);
    }

    // ==================== 通用HTTP方法 ====================

    private JsonNode doPost(String url, String apiKey, Map<String, Object> body)
    {
        try
        {
            String requestJson = objectMapper.writeValueAsString(body);
            log.info("Dify Knowledge API POST: url={}", url);

            HttpEntity<String> request = new HttpEntity<>(requestJson, buildHeaders(apiKey));
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null)
            {
                return objectMapper.readTree(response.getBody());
            }
            throw new RuntimeException("Dify API响应异常: " + response.getStatusCode());
        }
        catch (Exception e)
        {
            log.error("Dify Knowledge API POST失败: url={}", url, e);
            throw new RuntimeException("Dify Knowledge API调用失败: " + e.getMessage(), e);
        }
    }

    private JsonNode doGet(String url, String apiKey)
    {
        try
        {
            log.info("Dify Knowledge API GET: url={}", url);
            HttpEntity<String> request = new HttpEntity<>(buildHeaders(apiKey));
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, request, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null)
            {
                return objectMapper.readTree(response.getBody());
            }
            throw new RuntimeException("Dify API响应异常: " + response.getStatusCode());
        }
        catch (Exception e)
        {
            log.error("Dify Knowledge API GET失败: url={}", url, e);
            throw new RuntimeException("Dify Knowledge API调用失败: " + e.getMessage(), e);
        }
    }

    private boolean doDelete(String url, String apiKey)
    {
        try
        {
            log.info("Dify Knowledge API DELETE: url={}", url);
            HttpEntity<String> request = new HttpEntity<>(buildHeaders(apiKey));
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.DELETE, request, String.class);
            return response.getStatusCode().is2xxSuccessful();
        }
        catch (Exception e)
        {
            log.error("Dify Knowledge API DELETE失败: url={}", url, e);
            throw new RuntimeException("Dify Knowledge API调用失败: " + e.getMessage(), e);
        }
    }

    private HttpHeaders buildHeaders(String apiKey)
    {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);
        return headers;
    }
}
