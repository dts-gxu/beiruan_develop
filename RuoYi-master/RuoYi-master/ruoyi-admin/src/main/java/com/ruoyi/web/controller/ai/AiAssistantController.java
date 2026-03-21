package com.ruoyi.web.controller.ai;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.http.HttpServletRequest;
import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.utils.ShiroUtils;
import com.ruoyi.system.domain.AiChatConversation;
import com.ruoyi.system.domain.AiChatMessage;
import com.ruoyi.system.domain.AiDifyApp;
import com.ruoyi.system.service.IAiChatSessionService;
import com.ruoyi.system.service.IAiDifyAppService;
import com.ruoyi.system.domain.AiKnowledgeBase;
import com.ruoyi.system.domain.AiKnowledgeDocument;
import com.ruoyi.system.service.IAiKnowledgeBaseService;
import com.ruoyi.system.service.IAiKnowledgeDocumentService;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import jakarta.servlet.http.HttpServletResponse;

/**
 * AI开发助手 控制器
 * 
 * @author ruoyi
 */
@Controller
@RequestMapping("/ai/assistant")
public class AiAssistantController extends BaseController
{
    private static final Logger log = LoggerFactory.getLogger(AiAssistantController.class);

    private String prefix = "ai/assistant";

    @Autowired
    private AiChatService aiChatService;

    @Autowired
    private FileParseService fileParseService;

    @Autowired
    private ReportService reportService;

    @Autowired
    private DifyWorkflowService difyWorkflowService;

    @Autowired
    private IAiDifyAppService aiDifyAppService;

    @Autowired
    private IAiChatSessionService aiChatSessionService;

    @Autowired
    private DifyApiClient difyApiClient;

    @Autowired
    private IAiKnowledgeBaseService knowledgeBaseService;

    @Autowired
    private IAiKnowledgeDocumentService knowledgeDocumentService;

    @Autowired
    private DifyKnowledgeApiClient difyKnowledgeApi;

    /** Pipeline流式任务缓存（taskId -> [step, data]） */
    private final java.util.concurrent.ConcurrentHashMap<String, String[]> pendingPipelineTasks =
            new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * 需求分析页面
     */
    @RequiresPermissions("ai:assistant:view")
    @GetMapping()
    public String assistant()
    {
        return prefix + "/chat";
    }

    /**
     * AI同步聊天接口
     */
    @RequiresPermissions("ai:assistant:chat")
    @PostMapping("/chat")
    @ResponseBody
    public AjaxResult chat(@RequestParam("message") String message)
    {
        try
        {
            String reply = aiChatService.chat(message);
            return AjaxResult.success("操作成功", reply);
        }
        catch (Exception e)
        {
            log.error("AI聊天失败", e);
            return AjaxResult.error("AI服务异常：" + e.getMessage());
        }
    }

    /**
     * AI聊天（带会话持久化，仿FastGPT惰性创建模式）
     * conversationId可为null/0，此时自动创建会话并以第一条消息作为标题
     */
    @RequiresPermissions("ai:assistant:chat")
    @PostMapping("/chat/conversation")
    @ResponseBody
    public AjaxResult chatWithConversation(@RequestParam(value = "conversationId", required = false) Long conversationId,
                                            @RequestParam("content") String content)
    {
        try
        {
            long startTime = System.currentTimeMillis();
            Long userId = ShiroUtils.getUserId();

            // 惰性创建会话（仿FastGPT的upsert）
            AiChatConversation conversation = aiChatSessionService.ensureConversation(conversationId, userId, content);
            if (conversation == null)
            {
                return AjaxResult.error("创建会话失败");
            }
            Long actualConvId = conversation.getConversationId();

            // 验证用户归属
            if (!userId.equals(conversation.getUserId()))
            {
                return AjaxResult.error("会话不存在或无权访问");
            }

            // 保存用户消息
            aiChatSessionService.sendMessage(actualConvId, content);

            // 调用AI（普通对话使用通用提示词）
            String reply = aiChatService.chatGeneral(content);
            long costTime = System.currentTimeMillis() - startTime;

            // 保存AI回复
            aiChatSessionService.saveAiReply(actualConvId, reply, null, "DeepSeek V3", 0, (int) costTime);

            Map<String, Object> result = new HashMap<>();
            result.put("answer", reply);
            result.put("model", "DeepSeek V3");
            result.put("costTime", costTime);
            result.put("conversationId", actualConvId);
            return AjaxResult.success("操作成功", result);
        }
        catch (Exception e)
        {
            log.error("AI对话失败", e);
            return AjaxResult.error("对话失败：" + e.getMessage());
        }
    }

    /**
     * AI流式聊天接口（SSE） — 仿FastGPT OutputStream写UTF-8字节流
     */
    @RequiresPermissions("ai:assistant:chat")
    @GetMapping("/chat/stream")
    public void chatStream(@RequestParam("message") String message,
                           HttpServletRequest request, HttpServletResponse response)
    {
        SseHelper.initSseHeaders(response);
        AsyncContext asyncContext = SseHelper.startAsync(request, 180000);

        aiChatService.chatStreamGeneral(message, new StreamingChatResponseHandler()
        {
            @Override
            public void onPartialResponse(String partialResponse)
            {
                SseHelper.writeData(asyncContext, partialResponse);
            }

            @Override
            public void onCompleteResponse(ChatResponse completeResponse)
            {
                SseHelper.writeEvent(asyncContext, "done", "[DONE]");
                SseHelper.complete(asyncContext);
            }

            @Override
            public void onError(Throwable error)
            {
                log.error("AI流式聊天错误", error);
                SseHelper.complete(asyncContext);
            }
        });
    }

    /**
     * 需求分析 — 提取关键信息（步骤1.3）
     */
    @RequiresPermissions("ai:assistant:analyze")
    @PostMapping("/analyze")
    @ResponseBody
    public AjaxResult analyzeRequirement(@RequestParam("document") String document)
    {
        try
        {
            String result = aiChatService.analyzeRequirement(document);
            return AjaxResult.success("分析完成", result);
        }
        catch (Exception e)
        {
            log.error("需求分析失败", e);
            return AjaxResult.error("需求分析失败：" + e.getMessage());
        }
    }

    /**
     * RuoYi适配分析（步骤1.4）
     */
    @RequiresPermissions("ai:assistant:analyze")
    @PostMapping("/adapt")
    @ResponseBody
    public AjaxResult ruoyiAdaptAnalysis(@RequestParam("analysisResult") String analysisResult)
    {
        try
        {
            String result = aiChatService.ruoyiAdaptAnalysis(analysisResult);
            return AjaxResult.success("适配分析完成", result);
        }
        catch (Exception e)
        {
            log.error("适配分析失败", e);
            return AjaxResult.error("适配分析失败：" + e.getMessage());
        }
    }

    /**
     * 完整性评估（步骤1.5）
     */
    @RequiresPermissions("ai:assistant:analyze")
    @PostMapping("/evaluate")
    @ResponseBody
    public AjaxResult completenessEvaluation(@RequestParam("analysisResult") String analysisResult)
    {
        try
        {
            String result = aiChatService.completenessEvaluation(analysisResult);
            return AjaxResult.success("评估完成", result);
        }
        catch (Exception e)
        {
            log.error("完整性评估失败", e);
            return AjaxResult.error("完整性评估失败：" + e.getMessage());
        }
    }

    /**
     * 生成完整性报告（步骤1.6）
     */
    @RequiresPermissions("ai:assistant:analyze")
    @PostMapping("/report")
    @ResponseBody
    public AjaxResult generateReport(@RequestParam("analysisResult") String analysisResult)
    {
        try
        {
            String result = aiChatService.generateCompletenessReport(analysisResult);
            return AjaxResult.success("报告生成完成", result);
        }
        catch (Exception e)
        {
            log.error("报告生成失败", e);
            return AjaxResult.error("报告生成失败：" + e.getMessage());
        }
    }

    /**
     * 下载Word格式的完整性报告
     */
    @RequiresPermissions("ai:assistant:analyze")
    @PostMapping("/report/download")
    public void downloadReport(@RequestParam("reportContent") String reportContent,
                               HttpServletResponse response)
    {
        try
        {
            byte[] wordBytes = reportService.generateWordReport(reportContent, "需求完整性报告");

            response.setContentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
            String fileName = java.net.URLEncoder.encode("需求完整性报告_" + java.time.LocalDate.now() + ".docx", "UTF-8");
            response.setHeader("Content-Disposition", "attachment; filename=" + fileName);
            response.setContentLength(wordBytes.length);
            response.getOutputStream().write(wordBytes);
            response.getOutputStream().flush();
        }
        catch (Exception e)
        {
            log.error("Word报告下载失败", e);
            try
            {
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":500,\"msg\":\"报告下载失败：" + e.getMessage() + "\"}");
            }
            catch (Exception ex)
            {
                log.error("错误响应写入失败", ex);
            }
        }
    }

    /**
     * 运行Dify全流程工作流（一键执行）
     * 当Dify启用时，将需求文档发送给Dify工作流统一处理
     */
    @RequiresPermissions("ai:assistant:analyze")
    @PostMapping("/workflow/run")
    @ResponseBody
    public AjaxResult runDifyWorkflow(@RequestParam("document") String document)
    {
        try
        {
            if (!difyWorkflowService.isEnabled())
            {
                return AjaxResult.error("Dify工作流未启用，请在application.yml中配置dify.enabled=true并填入apiKey");
            }
            String result = difyWorkflowService.runRequirementAnalysis(document, "ruoyi-admin");
            return AjaxResult.success("工作流执行完成", result);
        }
        catch (Exception e)
        {
            log.error("Dify工作流执行失败", e);
            return AjaxResult.error("工作流执行失败：" + e.getMessage());
        }
    }

    /**
     * Dify对话接口（支持RAG知识库增强）
     */
    @RequiresPermissions("ai:assistant:chat")
    @PostMapping("/workflow/chat")
    @ResponseBody
    public AjaxResult difyChat(@RequestParam("message") String message,
                               @RequestParam(value = "conversationId", required = false) String conversationId)
    {
        try
        {
            if (!difyWorkflowService.isEnabled())
            {
                // 降级为直接LangChain4j调用
                String reply = aiChatService.chat(message);
                java.util.Map<String, String> result = new java.util.HashMap<>();
                result.put("answer", reply);
                result.put("conversationId", "");
                return AjaxResult.success("操作成功", result);
            }
            java.util.Map<String, String> result = difyWorkflowService.chat(message, "ruoyi-admin", conversationId);
            return AjaxResult.success("操作成功", result);
        }
        catch (Exception e)
        {
            log.error("Dify对话失败", e);
            return AjaxResult.error("对话失败：" + e.getMessage());
        }
    }

    // ==================== 知识库RAG对话 ====================

    /**
     * 带知识库引用的对话（RAG模式，支持多知识库）
     * 前端选择知识库后，将kbIds传入（逗号分隔），系统自动检索知识库文档内容注入上下文
     */
    @RequiresPermissions("ai:assistant:chat")
    @PostMapping("/chat/knowledge")
    @ResponseBody
    public AjaxResult chatWithKnowledge(@RequestParam(value = "conversationId", required = false) Long conversationId,
                                         @RequestParam("content") String content,
                                         @RequestParam("kbIds") String kbIds)
    {
        try
        {
            long startTime = System.currentTimeMillis();
            Long userId = ShiroUtils.getUserId();

            AiChatConversation conversation = aiChatSessionService.ensureConversation(conversationId, userId, content);
            if (conversation == null)
            {
                return AjaxResult.error("创建会话失败");
            }
            Long actualConvId = conversation.getConversationId();
            if (!userId.equals(conversation.getUserId()))
            {
                return AjaxResult.error("会话不存在或无权访问");
            }

            aiChatSessionService.sendMessage(actualConvId, content);

            // 构建多知识库上下文（优先Dify检索，fallback本地文档），同时收集引用段落
            log.info("【RAG-POST】开始检索: kbIds={}, content长度={}", kbIds, content.length());
            KnowledgeRetrievalResult retrieval = buildMultiKnowledgeContext(kbIds, content);
            log.info("【RAG-POST】检索完成: 上下文长度={}, quotes数量={}", 
                retrieval.context != null ? retrieval.context.length() : 0,
                retrieval.quotes != null ? retrieval.quotes.size() : 0);
            String reply = aiChatService.chatWithKnowledge(content, retrieval.context);
            long costTime = System.currentTimeMillis() - startTime;

            // 调试日志：检查AI是否生成了[N]引用标记
            String replyPreview = reply.length() > 400 ? reply.substring(0, 400) : reply;
            log.info("【RAG引用调试】AI回复前400字符: {}", replyPreview);
            log.info("【RAG引用调试】AI回复是否含[N]标记: {}", reply.matches("(?s).*\\[\\d{1,2}\\].*"));

            aiChatSessionService.saveAiReply(actualConvId, reply, null, "DeepSeek V3", 0, (int) costTime);

            Map<String, Object> result = new HashMap<>();
            result.put("answer", reply);
            result.put("model", "DeepSeek V3");
            result.put("costTime", costTime);
            result.put("conversationId", actualConvId);
            result.put("kbIds", kbIds);
            result.put("quotes", retrieval.quotes);
            return AjaxResult.success("操作成功", result);
        }
        catch (Exception e)
        {
            log.error("知识库RAG对话失败", e);
            return AjaxResult.error("对话失败：" + e.getMessage());
        }
    }

    /**
     * 带知识库引用的流式对话（RAG + SSE，支持多知识库）
     * 完整流程：惰性创建会话 → 保存用户消息 → 检索知识库 → 发送quotes/conversationId → 流式AI回复 → 保存AI回复
     */
    @RequiresPermissions("ai:assistant:chat")
    @GetMapping("/chat/stream/knowledge")
    public void chatStreamWithKnowledge(@RequestParam("message") String message,
                                         @RequestParam("kbIds") String kbIds,
                                         @RequestParam(value = "conversationId", required = false) Long conversationId,
                                         HttpServletRequest request, HttpServletResponse response)
    {
        SseHelper.initSseHeaders(response);
        Long userId = ShiroUtils.getUserId();

        // 惰性创建会话
        AiChatConversation conversation = aiChatSessionService.ensureConversation(conversationId, userId, message);
        if (conversation == null)
        {
            SseHelper.writeEvent(response, "error", "创建会话失败");
            return;
        }
        Long actualConvId = conversation.getConversationId();

        // 保存用户消息
        aiChatSessionService.sendMessage(actualConvId, message);

        AsyncContext asyncContext = SseHelper.startAsync(request, 180000);

        // 先发送会话ID（前端用于更新状态）
        SseHelper.writeEvent(asyncContext, "conversationId", String.valueOf(actualConvId));

        try
        {
            // 检索知识库上下文，同时收集引用段落
            log.info("【RAG-SSE】开始检索: kbIds={}, message长度={}", kbIds, message.length());
            KnowledgeRetrievalResult retrieval = buildMultiKnowledgeContext(kbIds, message);
            log.info("【RAG-SSE】检索完成: 上下文长度={}, quotes数量={}", 
                retrieval.context != null ? retrieval.context.length() : 0,
                retrieval.quotes != null ? retrieval.quotes.size() : 0);

            // 发送引用段落数据（quotes事件），前端渲染引用卡片
            if (retrieval.quotes != null && !retrieval.quotes.isEmpty())
            {
                try
                {
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    String quotesJson = mapper.writeValueAsString(retrieval.quotes);
                    SseHelper.writeEvent(asyncContext, "quotes", quotesJson);
                }
                catch (Exception jsonEx)
                {
                    log.warn("序列化quotes失败: {}", jsonEx.getMessage());
                }
            }

            long startTime = System.currentTimeMillis();
            StringBuilder fullResponse = new StringBuilder();
            java.util.concurrent.atomic.AtomicBoolean replySaved = new java.util.concurrent.atomic.AtomicBoolean(false);

            Runnable savePartialResponse = () -> {
                if (replySaved.compareAndSet(false, true) && fullResponse.length() > 0)
                {
                    try
                    {
                        long costTime = System.currentTimeMillis() - startTime;
                        aiChatSessionService.saveAiReply(actualConvId, fullResponse.toString(),
                                null, "DeepSeek V3", 0, (int) costTime);
                        log.info("RAG流式：已保存AI响应（{}字符）到会话{}", fullResponse.length(), actualConvId);
                    }
                    catch (Exception ex)
                    {
                        log.error("RAG流式：保存AI响应失败", ex);
                    }
                }
            };

            aiChatService.chatStreamWithKnowledge(message, retrieval.context, new StreamingChatResponseHandler()
            {
                @Override
                public void onPartialResponse(String partialResponse)
                {
                    fullResponse.append(partialResponse);
                    SseHelper.writeData(asyncContext, partialResponse);
                }

                @Override
                public void onCompleteResponse(ChatResponse completeResponse)
                {
                    // 调试日志：检查AI是否生成了[N]引用标记
                    String fullText = fullResponse.toString();
                    String preview = fullText.length() > 400 ? fullText.substring(0, 400) : fullText;
                    log.info("【RAG-SSE引用调试】AI回复前400字符: {}", preview);
                    log.info("【RAG-SSE引用调试】AI回复是否含[N]标记: {}", fullText.matches("(?s).*\\[\\d{1,2}\\].*"));

                    if (replySaved.compareAndSet(false, true))
                    {
                        long costTime = System.currentTimeMillis() - startTime;
                        try {
                            aiChatSessionService.saveAiReply(actualConvId, fullText,
                                    null, "DeepSeek V3", 0, (int) costTime);
                        } catch (Exception ex) {
                            log.error("RAG流式：保存AI响应失败", ex);
                        }
                    }
                    SseHelper.writeEvent(asyncContext, "done", "[DONE]");
                    SseHelper.complete(asyncContext);
                }

                @Override
                public void onError(Throwable error)
                {
                    log.error("知识库RAG流式聊天错误", error);
                    savePartialResponse.run();
                    SseHelper.complete(asyncContext);
                }
            });
        }
        catch (Exception e)
        {
            log.error("知识库RAG流式聊天失败", e);
            SseHelper.complete(asyncContext);
        }
    }

    /**
     * 获取可用知识库列表（供前端下拉选择）
     */
    @RequiresPermissions("ai:assistant:chat")
    @PostMapping("/knowledge/list")
    @ResponseBody
    public AjaxResult listKnowledgeBases()
    {
        AiKnowledgeBase query = new AiKnowledgeBase();
        query.setStatus("0");
        List<AiKnowledgeBase> list = knowledgeBaseService.selectAiKnowledgeBaseList(query);
        return AjaxResult.success("操作成功", list);
    }

    /**
     * 构建多知识库上下文（支持逗号分隔的多个kbId）
     * 对每个知识库独立调用Dify检索API，合并结果，同时收集引用段落
     */
    private KnowledgeRetrievalResult buildMultiKnowledgeContext(String kbIds, String userQuery)
    {
        List<Map<String, Object>> allQuotes = new ArrayList<>();
        if (kbIds == null || kbIds.trim().isEmpty())
        {
            return new KnowledgeRetrievalResult("（未选择知识库）", allQuotes);
        }

        String[] idArr = kbIds.split(",");
        int maxTotalLength = 12000;
        int currentTotalLength = 0;
        int totalHits = 0;

        // 先收集所有知识库的检索结果
        for (String idStr : idArr)
        {
            if (currentTotalLength >= maxTotalLength) break;
            Long kbId;
            try { kbId = Long.parseLong(idStr.trim()); }
            catch (NumberFormatException e) { continue; }

            KnowledgeRetrievalResult singleResult = buildSingleKnowledgeContext(kbId, userQuery, maxTotalLength - currentTotalLength);
            if (singleResult.context != null && !singleResult.context.isEmpty())
            {
                currentTotalLength += singleResult.context.length();
                allQuotes.addAll(singleResult.quotes);
                totalHits++;
            }
        }

        if (totalHits == 0)
        {
            return new KnowledgeRetrievalResult("（知识库检索无结果）", allQuotes);
        }

        // 统一编号并构建带编号的上下文（[1] [2] [3]...让AI做内联引用）
        StringBuilder numberedContext = new StringBuilder();
        for (int i = 0; i < allQuotes.size(); i++)
        {
            Map<String, Object> quote = allQuotes.get(i);
            int idx = i + 1;
            quote.put("index", idx);
            numberedContext.append("[").append(idx).append("] ");
            numberedContext.append("文档：").append(quote.get("docName"));
            numberedContext.append("（").append(quote.get("kbName")).append("）\n");
            numberedContext.append(quote.get("content")).append("\n\n");
        }

        log.info("多知识库检索完成: kbIds={}, 命中{}个知识库, 引用{}段, 总上下文{}字符", kbIds, totalHits, allQuotes.size(), numberedContext.length());
        return new KnowledgeRetrievalResult(numberedContext.toString(), allQuotes);
    }

    /**
     * 构建单个知识库上下文（优先Dify检索API，fallback本地文档内容）
     * 参考 max-serve DatasetApiService.searchTest 模式
     * 返回 KnowledgeRetrievalResult 包含上下文文本和引用段落列表
     */
    private KnowledgeRetrievalResult buildSingleKnowledgeContext(Long kbId, String userQuery, int maxLength)
    {
        List<Map<String, Object>> quotes = new ArrayList<>();
        AiKnowledgeBase kb = knowledgeBaseService.selectAiKnowledgeBaseByKbId(kbId);
        if (kb == null)
        {
            return new KnowledgeRetrievalResult(null, quotes);
        }

        // 策略1：优先使用Dify原生检索API（向量检索+重排序，效果最好）
        if (isNotEmpty(kb.getDifyDatasetId()) && isNotEmpty(kb.getDifyApiKey()) && isNotEmpty(kb.getDifyBaseUrl()))
        {
            try
            {
                // Dify API query字段有Pydantic max_length约200限制，截断避免string_too_long错误
                // 注意：这只是搜索查询的限制，不影响文档上传大小
                String truncatedQuery = userQuery.length() > 200 ? userQuery.substring(0, 200) : userQuery;
                com.fasterxml.jackson.databind.JsonNode result = difyKnowledgeApi.retrieveDataset(
                    kb.getDifyBaseUrl(), kb.getDifyApiKey(), kb.getDifyDatasetId(), truncatedQuery
                );
                com.fasterxml.jackson.databind.JsonNode records = result.path("records");
                if (records.isArray() && records.size() > 0)
                {
                    StringBuilder context = new StringBuilder();
                    context.append("=== 知识库：").append(kb.getKbName()).append(" ===").append("\n\n");
                    int currentLength = 0;
                    for (com.fasterxml.jackson.databind.JsonNode record : records)
                    {
                        if (currentLength >= maxLength) break;
                        com.fasterxml.jackson.databind.JsonNode segment = record.path("segment");
                        String content = segment.path("content").asText("");
                        // document嵌套在segment内部：record.segment.document.name
                        com.fasterxml.jackson.databind.JsonNode docNode = segment.path("document");
                        String docName = docNode.path("name").asText("未命名");
                        double score = record.path("score").asDouble(0);
                        log.debug("Dify检索段: docName={}, score={}, contentLen={}, docId={}", docName, score, content.length(), docNode.path("id").asText(""));
                        if (!content.isEmpty())
                        {
                            String originalContent = content;
                            if (currentLength + content.length() > maxLength)
                            {
                                content = content.substring(0, maxLength - currentLength);
                            }
                            context.append("--- 文档：").append(docName)
                                .append("（相关度：").append(String.format("%.2f", score)).append("） ---\n");
                            context.append(content).append("\n\n");
                            currentLength += content.length();

                            // 收集引用段落信息（供前端展示引用卡片+查看全文+下载）
                            Map<String, Object> quote = new HashMap<>();
                            quote.put("docName", docName);
                            quote.put("content", originalContent.length() > 1000 ? originalContent.substring(0, 1000) : originalContent);
                            quote.put("score", score);
                            quote.put("kbName", kb.getKbName());
                            quote.put("kbId", kbId);
                            // 获取Dify文档ID，用于反查本地文档
                            String difyDocId = docNode.path("id").asText("");
                            // 备用：segment级别的document_id
                            if (difyDocId.isEmpty()) difyDocId = segment.path("document_id").asText("");
                            quote.put("difyDocumentId", difyDocId);
                            // 尝试通过difyDocumentId反查本地文档，获取docId和filePath
                            if (!difyDocId.isEmpty())
                            {
                                try
                                {
                                    AiKnowledgeDocument localDoc = knowledgeDocumentService.selectByDifyDocumentId(difyDocId);
                                    if (localDoc != null)
                                    {
                                        quote.put("docId", localDoc.getDocId());
                                        quote.put("filePath", localDoc.getFilePath());
                                        quote.put("docType", localDoc.getDocType());
                                        // 如果Dify返回的docName为空或未命名，使用本地文档名
                                        if ("未命名".equals(docName) || docName.isEmpty())
                                        {
                                            quote.put("docName", localDoc.getDocName());
                                        }
                                    }
                                }
                                catch (Exception ex)
                                {
                                    log.debug("通过difyDocumentId反查本地文档失败: {}", ex.getMessage());
                                }
                            }
                            quotes.add(quote);
                        }
                    }
                    log.info("Dify知识库检索成功: kbId={}, kbName={}, 命中{}条, 上下文{}字符",
                        kbId, kb.getKbName(), records.size(), currentLength);
                    return new KnowledgeRetrievalResult(context.toString(), quotes);
                }
            }
            catch (Exception e)
            {
                log.warn("Dify知识库检索失败，降级使用本地文档: kbId={}, error={}", kbId, e.getMessage());
            }
        }

        // 策略2：降级使用本地文档内容
        return buildLocalKnowledgeContext(kb);
    }

    /**
     * 从本地数据库文档构建知识库上下文（fallback策略）
     */
    private KnowledgeRetrievalResult buildLocalKnowledgeContext(AiKnowledgeBase kb)
    {
        List<Map<String, Object>> quotes = new ArrayList<>();
        List<AiKnowledgeDocument> docs = knowledgeDocumentService.selectAiKnowledgeDocumentListWithContent(kb.getKbId());

        if (docs.isEmpty())
        {
            return new KnowledgeRetrievalResult("（知识库[" + kb.getKbName() + "]中暂无文档）", quotes);
        }

        StringBuilder context = new StringBuilder();
        context.append("知识库名称：").append(kb.getKbName()).append("\n\n");

        int maxContextLength = 8000;
        int currentLength = 0;

        for (AiKnowledgeDocument doc : docs)
        {
            if (currentLength >= maxContextLength) break;
            if (doc.getContent() != null && !doc.getContent().isEmpty())
            {
                String snippet = doc.getContent();
                if (currentLength + snippet.length() > maxContextLength)
                {
                    snippet = snippet.substring(0, maxContextLength - currentLength);
                }
                context.append("--- 文档：").append(doc.getDocName()).append(" ---\n");
                context.append(snippet).append("\n\n");
                currentLength += snippet.length();

                // 收集引用段落（本地文档含完整信息，用于前端查看全文+下载）
                Map<String, Object> quote = new HashMap<>();
                quote.put("docName", doc.getDocName());
                quote.put("content", snippet.length() > 1000 ? snippet.substring(0, 1000) : snippet);
                quote.put("score", 0.5);
                quote.put("kbName", kb.getKbName());
                quote.put("kbId", kb.getKbId());
                quote.put("docId", doc.getDocId());
                quote.put("docType", doc.getDocType());
                quote.put("filePath", doc.getFilePath());
                quotes.add(quote);
            }
            else
            {
                context.append("--- 文档：").append(doc.getDocName())
                    .append("（类型：").append(doc.getDocType())
                    .append("，索引状态：").append(doc.getIndexingStatus())
                    .append("） ---\n");
            }
        }

        return new KnowledgeRetrievalResult(context.toString(), quotes);
    }

    private boolean isNotEmpty(String str)
    {
        return str != null && !str.trim().isEmpty();
    }

    /** 知识库检索结果（包含上下文文本+引用段落列表） */
    private static class KnowledgeRetrievalResult
    {
        String context;
        List<Map<String, Object>> quotes;

        KnowledgeRetrievalResult(String context, List<Map<String, Object>> quotes)
        {
            this.context = context;
            this.quotes = quotes;
        }
    }

    /**
     * 获取知识库文档全文内容（供前端引用查看全文功能）
     */
    @RequiresPermissions("ai:assistant:chat")
    @GetMapping("/knowledge/document/content")
    @ResponseBody
    public AjaxResult getDocumentContent(@RequestParam("docId") Long docId)
    {
        AiKnowledgeDocument doc = knowledgeDocumentService.selectAiKnowledgeDocumentByDocId(docId);
        if (doc == null)
        {
            return AjaxResult.error("文档不存在");
        }
        Map<String, Object> result = new HashMap<>();
        result.put("docId", doc.getDocId());
        result.put("docName", doc.getDocName());
        result.put("docType", doc.getDocType());
        result.put("content", doc.getContent());
        result.put("wordCount", doc.getWordCount());
        result.put("filePath", doc.getFilePath());
        return AjaxResult.success("操作成功", result);
    }

    /**
     * 下载知识库文档原始文件
     */
    @RequiresPermissions("ai:assistant:chat")
    @GetMapping("/knowledge/document/download")
    public void downloadDocument(@RequestParam("docId") Long docId, HttpServletResponse response)
    {
        try
        {
            AiKnowledgeDocument doc = knowledgeDocumentService.selectAiKnowledgeDocumentByDocId(docId);
            if (doc == null)
            {
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":500,\"msg\":\"文档不存在\"}");
                return;
            }
            String filePath = doc.getFilePath();
            if (filePath == null || filePath.isEmpty())
            {
                // 没有原始文件，将content作为txt下载
                String content = doc.getContent();
                if (content == null || content.isEmpty())
                {
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"code\":500,\"msg\":\"该文档无可下载内容\"}");
                    return;
                }
                String fileName = doc.getDocName();
                if (fileName == null || fileName.isEmpty()) fileName = "document";
                if (!fileName.endsWith(".txt")) fileName += ".txt";
                response.setContentType("text/plain;charset=UTF-8");
                response.setHeader("Content-Disposition", "attachment; filename=\"" +
                    java.net.URLEncoder.encode(fileName, "UTF-8").replace("+", "%20") + "\"");
                response.getWriter().write(content);
                return;
            }
            // 有原始文件路径，读取并下载
            // filePath可能是相对于RuoYi上传目录的路径
            String realPath = filePath;
            if (!new File(realPath).isAbsolute())
            {
                realPath = RuoYiConfig.getProfile() + File.separator + filePath;
            }
            File file = new File(realPath);
            if (!file.exists())
            {
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":500,\"msg\":\"文件不存在：" + doc.getDocName() + "\"}");
                return;
            }
            String fileName = doc.getDocName();
            if (fileName == null || fileName.isEmpty()) fileName = file.getName();
            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition", "attachment; filename=\"" +
                java.net.URLEncoder.encode(fileName, "UTF-8").replace("+", "%20") + "\"");
            response.setContentLengthLong(file.length());
            try (FileInputStream fis = new FileInputStream(file); OutputStream os = response.getOutputStream())
            {
                byte[] buf = new byte[4096];
                int len;
                while ((len = fis.read(buf)) != -1)
                {
                    os.write(buf, 0, len);
                }
                os.flush();
            }
        }
        catch (Exception e)
        {
            log.error("下载知识库文档失败: docId={}", docId, e);
            try
            {
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":500,\"msg\":\"下载失败\"}");
            }
            catch (Exception ignored) {}
        }
    }

    /**
     * 检查Dify工作流是否可用
     */
    @RequiresPermissions("ai:assistant:chat")
    @PostMapping("/workflow/status")
    @ResponseBody
    public AjaxResult difyStatus()
    {
        java.util.Map<String, Object> status = new java.util.HashMap<>();
        status.put("difyEnabled", difyWorkflowService.isEnabled());
        status.put("mode", difyWorkflowService.isEnabled() ? "dify" : "langchain4j");
        return AjaxResult.success("操作成功", status);
    }

    /**
     * 上传需求文件（支持PDF/Word/Excel/图片/TXT）
     * 文件按用户隔离存储到 {profile}/ai/{userId}/ 目录
     */
    @RequiresPermissions("ai:assistant:analyze")
    @PostMapping("/upload")
    @ResponseBody
    public AjaxResult uploadFile(@RequestParam("file") MultipartFile file)
    {
        try
        {
            if (file.isEmpty())
            {
                return AjaxResult.error("请选择要上传的文件");
            }
            long maxSize = 20 * 1024 * 1024L;
            if (file.getSize() > maxSize)
            {
                return AjaxResult.error("文件大小不能超过20MB");
            }

            // 解析文件内容
            String text = fileParseService.parseFile(file);
            if (text == null || text.trim().isEmpty())
            {
                return AjaxResult.error("文件内容为空，无法提取文本");
            }

            // 按用户隔离存储原始文件
            Long userId = ShiroUtils.getUserId();
            String userDir = RuoYiConfig.getProfile() + "/ai/" + userId;
            Path dirPath = Paths.get(userDir);
            Files.createDirectories(dirPath);

            // 生成唯一文件名防止覆盖
            String originalName = file.getOriginalFilename();
            String ext = "";
            if (originalName != null && originalName.contains("."))
            {
                ext = originalName.substring(originalName.lastIndexOf("."));
            }
            String storedName = System.currentTimeMillis() + "_" + (originalName != null ? originalName : "file" + ext);
            Path filePath = dirPath.resolve(storedName);
            file.transferTo(filePath.toFile());

            log.info("文件上传解析成功：{}，存储路径：{}，提取文本长度：{}", originalName, filePath, text.length());

            // 返回解析文本和文件下载信息
            Map<String, Object> result = new HashMap<>();
            result.put("text", text);
            result.put("fileName", originalName);
            result.put("storedName", storedName);
            result.put("fileSize", file.getSize());
            return AjaxResult.success("文件解析成功", result);
        }
        catch (Exception e)
        {
            log.error("文件上传解析失败", e);
            return AjaxResult.error("文件解析失败：" + e.getMessage());
        }
    }

    /**
     * 下载用户上传的文件（按用户隔离）
     */
    @RequiresPermissions("ai:assistant:analyze")
    @GetMapping("/download")
    public void downloadFile(@RequestParam("storedName") String storedName,
                             @RequestParam(value = "fileName", required = false) String fileName,
                             HttpServletResponse response)
    {
        try
        {
            Long userId = ShiroUtils.getUserId();
            String userDir = RuoYiConfig.getProfile() + "/ai/" + userId;
            Path filePath = Paths.get(userDir, storedName).normalize();

            // 安全校验：确保文件在用户目录内
            if (!filePath.startsWith(Paths.get(userDir)))
            {
                response.setStatus(403);
                return;
            }

            File file = filePath.toFile();
            if (!file.exists())
            {
                response.setStatus(404);
                return;
            }

            String downloadName = (fileName != null && !fileName.isEmpty()) ? fileName : storedName;
            String encodedName = java.net.URLEncoder.encode(downloadName, "UTF-8").replaceAll("\\+", "%20");
            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition", "attachment; filename=" + encodedName);
            response.setContentLengthLong(file.length());

            try (FileInputStream fis = new FileInputStream(file); OutputStream os = response.getOutputStream())
            {
                byte[] buffer = new byte[4096];
                int len;
                while ((len = fis.read(buffer)) != -1)
                {
                    os.write(buffer, 0, len);
                }
                os.flush();
            }
        }
        catch (Exception e)
        {
            log.error("文件下载失败", e);
            try
            {
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":500,\"msg\":\"文件下载失败\"}");
            }
            catch (Exception ex)
            {
                log.error("错误响应写入失败", ex);
            }
        }
    }

    // ==================== Dify对话管理接口 ====================

    /**
     * 获取可用的Dify应用列表
     */
    @PostMapping("/dify/apps")
    @ResponseBody
    public AjaxResult getDifyApps()
    {
        List<AiDifyApp> list = aiDifyAppService.selectActiveAppList();
        return AjaxResult.success(list);
    }

    /**
     * 创建新会话
     */
    @PostMapping("/conversation/create")
    @ResponseBody
    public AjaxResult createConversation(@RequestParam(value = "appId", required = false) Long appId,
                                          @RequestParam(value = "title", required = false) String title,
                                          @RequestParam(value = "conversationType", required = false) String conversationType)
    {
        try
        {
            Long userId = ShiroUtils.getUserId();
            AiChatConversation conversation = aiChatSessionService.createConversation(
                appId != null ? appId : 0L, userId, title, conversationType);
            return AjaxResult.success("创建成功", conversation);
        }
        catch (Exception e)
        {
            log.error("创建会话失败", e);
            return AjaxResult.error("创建会话失败：" + e.getMessage());
        }
    }

    /**
     * 查询用户的会话列表
     */
    @PostMapping("/conversation/list")
    @ResponseBody
    public AjaxResult listConversations(@RequestParam(value = "appId", required = false) Long appId)
    {
        try
        {
            AiChatConversation query = new AiChatConversation();
            query.setUserId(ShiroUtils.getUserId());
            if (appId != null)
            {
                query.setAppId(appId);
            }
            List<AiChatConversation> list = aiChatSessionService.selectConversationList(query);
            return AjaxResult.success(list);
        }
        catch (Exception e)
        {
            log.error("获取会话列表失败", e);
            return AjaxResult.error("获取会话列表失败：" + e.getMessage());
        }
    }

    /**
     * 获取会话的消息历史（验证用户归属）
     */
    @PostMapping("/conversation/messages")
    @ResponseBody
    public AjaxResult getConversationMessages(@RequestParam("conversationId") Long conversationId)
    {
        try
        {
            // 验证会话归属当前用户
            AiChatConversation conv = aiChatSessionService.selectConversationById(conversationId);
            if (conv == null || !ShiroUtils.getUserId().equals(conv.getUserId()))
            {
                return AjaxResult.error("会话不存在或无权访问");
            }
            List<AiChatMessage> messages = aiChatSessionService.selectMessagesByConversationId(conversationId);
            return AjaxResult.success(messages);
        }
        catch (Exception e)
        {
            log.error("获取消息列表失败", e);
            return AjaxResult.error("获取消息列表失败：" + e.getMessage());
        }
    }

    /**
     * 删除会话（含消息，验证用户归属）
     */
    @PostMapping("/conversation/delete")
    @ResponseBody
    public AjaxResult deleteConversation(@RequestParam("conversationId") Long conversationId)
    {
        try
        {
            AiChatConversation conv = aiChatSessionService.selectConversationById(conversationId);
            if (conv == null || !ShiroUtils.getUserId().equals(conv.getUserId()))
            {
                return AjaxResult.error("会话不存在或无权操作");
            }
            aiChatSessionService.deleteConversation(conversationId);
            return AjaxResult.success("删除成功");
        }
        catch (Exception e)
        {
            log.error("删除会话失败", e);
            return AjaxResult.error("删除失败：" + e.getMessage());
        }
    }

    /**
     * 修改会话标题（验证用户归属）
     */
    @PostMapping("/conversation/rename")
    @ResponseBody
    public AjaxResult renameConversation(@RequestParam("conversationId") Long conversationId,
                                          @RequestParam("title") String title)
    {
        try
        {
            AiChatConversation conv = aiChatSessionService.selectConversationById(conversationId);
            if (conv == null || !ShiroUtils.getUserId().equals(conv.getUserId()))
            {
                return AjaxResult.error("会话不存在或无权操作");
            }
            aiChatSessionService.updateConversationTitle(conversationId, title);
            return AjaxResult.success("修改成功");
        }
        catch (Exception e)
        {
            log.error("修改会话标题失败", e);
            return AjaxResult.error("修改失败：" + e.getMessage());
        }
    }

    /**
     * 切换会话置顶（仿FastGPT updateHistory的top字段）
     */
    @PostMapping("/conversation/toggleTop")
    @ResponseBody
    public AjaxResult toggleTopConversation(@RequestParam("conversationId") Long conversationId,
                                             @RequestParam("top") Integer top)
    {
        try
        {
            AiChatConversation conv = aiChatSessionService.selectConversationById(conversationId);
            if (conv == null || !ShiroUtils.getUserId().equals(conv.getUserId()))
            {
                return AjaxResult.error("会话不存在或无权操作");
            }
            aiChatSessionService.toggleTopConversation(conversationId, top);
            return AjaxResult.success("操作成功");
        }
        catch (Exception e)
        {
            log.error("切换置顶失败", e);
            return AjaxResult.error("操作失败：" + e.getMessage());
        }
    }

    /**
     * 清空用户所有对话历史（仿FastGPT clearHistories，软删除）
     */
    @PostMapping("/conversation/clearAll")
    @ResponseBody
    public AjaxResult clearAllConversations()
    {
        try
        {
            Long userId = ShiroUtils.getUserId();
            aiChatSessionService.clearAllConversations(userId);
            return AjaxResult.success("清空成功");
        }
        catch (Exception e)
        {
            log.error("清空对话历史失败", e);
            return AjaxResult.error("清空失败：" + e.getMessage());
        }
    }

    /**
     * Pipeline流式分析 — 第1步: POST提交数据（避免GET URL过长）
     * 返回taskId，前端用taskId连接SSE流
     */
    @RequiresPermissions("ai:assistant:analyze")
    @PostMapping("/pipeline/prepare")
    @ResponseBody
    public AjaxResult preparePipeline(@RequestParam("step") String step,
                                       @RequestParam("data") String data,
                                       @RequestParam(value = "kbIds", required = false) String kbIds)
    {
        String taskId = java.util.UUID.randomUUID().toString().substring(0, 12);
        pendingPipelineTasks.put(taskId, new String[]{step, data, kbIds != null ? kbIds : ""});
        // 30秒后自动清理未使用的任务
        new Thread(() -> {
            try { Thread.sleep(30000); } catch (InterruptedException ignored) {}
            pendingPipelineTasks.remove(taskId);
        }).start();
        Map<String, Object> result = new HashMap<>();
        result.put("taskId", taskId);
        return AjaxResult.success(result);
    }

    /**
     * Pipeline流式分析 — 仿FastGPT responseWrite: OutputStream写UTF-8字节流
     */
    @RequiresPermissions("ai:assistant:analyze")
    @GetMapping("/pipeline/stream")
    public void pipelineStream(@RequestParam("taskId") String taskId,
                               HttpServletRequest request, HttpServletResponse response)
    {
        SseHelper.initSseHeaders(response);

        String[] task = pendingPipelineTasks.remove(taskId);
        if (task == null)
        {
            SseHelper.writeEvent(response, "error", "任务不存在或已过期");
            return;
        }

        String step = task[0];
        String data = task[1];
        String kbIds = task.length > 2 ? task[2] : "";

        // 如果选择了知识库，先检索相关上下文并发送引用段落
        String knowledgeContext = "";
        AsyncContext asyncContext = SseHelper.startAsync(request, 300000);
        if (kbIds != null && !kbIds.trim().isEmpty())
        {
            try
            {
                KnowledgeRetrievalResult retrieval = buildMultiKnowledgeContext(kbIds, data.length() > 500 ? data.substring(0, 500) : data);
                if (retrieval.context != null && !retrieval.context.isEmpty())
                {
                    knowledgeContext = retrieval.context;
                }
                if (retrieval.quotes != null && !retrieval.quotes.isEmpty())
                {
                    try
                    {
                        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                        String quotesJson = mapper.writeValueAsString(retrieval.quotes);
                        SseHelper.writeEvent(asyncContext, "quotes", quotesJson);
                    }
                    catch (Exception jsonEx)
                    {
                        log.warn("Pipeline序列化quotes失败: {}", jsonEx.getMessage());
                    }
                }
            }
            catch (Exception e)
            {
                log.warn("Pipeline知识库检索失败: {}", e.getMessage());
            }
        }

        // 构建Pipeline提示词，如果有知识库上下文则附加
        String prompt = aiChatService.buildPipelinePrompt(step, data);
        if (prompt == null || prompt.isEmpty())
        {
            SseHelper.writeEvent(asyncContext, "error", "无效的分析步骤");
            SseHelper.complete(asyncContext);
            return;
        }
        if (!knowledgeContext.isEmpty())
        {
            prompt = prompt + "\n\n【参考知识库资料】\n" + knowledgeContext;
        }

        StringBuilder fullResponse = new StringBuilder();
        java.util.concurrent.atomic.AtomicBoolean completed = new java.util.concurrent.atomic.AtomicBoolean(false);

        aiChatService.chatStreamWithContinuation(prompt, null, new StreamingChatResponseHandler()
        {
            @Override
            public void onPartialResponse(String partialResponse)
            {
                fullResponse.append(partialResponse);
                SseHelper.writeData(asyncContext, partialResponse);
            }

            @Override
            public void onCompleteResponse(ChatResponse completeResponse)
            {
                if (completed.compareAndSet(false, true))
                {
                    SseHelper.writeEvent(asyncContext, "done", fullResponse.toString());
                    SseHelper.complete(asyncContext);
                }
            }

            @Override
            public void onError(Throwable error)
            {
                log.error("Pipeline流式分析错误", error);
                if (completed.compareAndSet(false, true))
                {
                    if (fullResponse.length() > 0)
                    {
                        SseHelper.writeEvent(asyncContext, "done", fullResponse.toString());
                    }
                    else
                    {
                        SseHelper.writeEvent(asyncContext, "error", error.getMessage());
                    }
                    SseHelper.complete(asyncContext);
                }
            }
        });
    }

    /**
     * 保存消息对到会话（供pipeline分析步骤持久化使用）
     * 仿FastGPT: 所有消息都通过会话系统持久化
     */
    @RequiresPermissions("ai:assistant:chat")
    @PostMapping("/conversation/saveMessages")
    @ResponseBody
    public AjaxResult saveMessagesToConversation(
            @RequestParam(value = "conversationId", required = false) Long conversationId,
            @RequestParam("userMessage") String userMessage,
            @RequestParam("aiReply") String aiReply)
    {
        try
        {
            Long userId = ShiroUtils.getUserId();
            // 惰性创建会话
            AiChatConversation conversation = aiChatSessionService.ensureConversation(conversationId, userId, userMessage);
            if (conversation == null)
            {
                return AjaxResult.error("创建会话失败");
            }
            Long actualConvId = conversation.getConversationId();
            // 保存用户消息和AI回复
            aiChatSessionService.sendMessage(actualConvId, userMessage);
            aiChatSessionService.saveAiReply(actualConvId, aiReply, null, "DeepSeek V3", 0, 0);

            Map<String, Object> result = new HashMap<>();
            result.put("conversationId", actualConvId);
            return AjaxResult.success("保存成功", result);
        }
        catch (Exception e)
        {
            log.error("保存消息失败", e);
            return AjaxResult.error("保存失败：" + e.getMessage());
        }
    }

    /**
     * SSE流式聊天 — 仿FastGPT responseWrite: OutputStream写UTF-8字节流
     */
    @RequiresPermissions("ai:assistant:chat")
    @GetMapping("/chat/stream/conversation")
    public void chatStreamWithConversation(
            @RequestParam(value = "conversationId", required = false) Long conversationId,
            @RequestParam("message") String message,
            HttpServletRequest request, HttpServletResponse response)
    {
        SseHelper.initSseHeaders(response);
        Long userId = ShiroUtils.getUserId();

        // 惰性创建会话
        AiChatConversation conversation = aiChatSessionService.ensureConversation(conversationId, userId, message);
        if (conversation == null)
        {
            SseHelper.writeEvent(response, "error", "创建会话失败");
            return;
        }
        Long actualConvId = conversation.getConversationId();

        // 保存用户消息
        aiChatSessionService.sendMessage(actualConvId, message);

        AsyncContext asyncContext = SseHelper.startAsync(request, 180000);

        // 先发送会话ID
        SseHelper.writeEvent(asyncContext, "conversationId", String.valueOf(actualConvId));

        long startTime = System.currentTimeMillis();
        StringBuilder fullResponse = new StringBuilder();
        java.util.concurrent.atomic.AtomicBoolean replySaved = new java.util.concurrent.atomic.AtomicBoolean(false);

        Runnable savePartialResponse = () -> {
            if (replySaved.compareAndSet(false, true) && fullResponse.length() > 0)
            {
                try
                {
                    long costTime = System.currentTimeMillis() - startTime;
                    aiChatSessionService.saveAiReply(actualConvId, fullResponse.toString(),
                            null, "DeepSeek V3", 0, (int) costTime);
                    log.info("已保存部分AI响应（{}字符）到会话{}", fullResponse.length(), actualConvId);
                }
                catch (Exception ex)
                {
                    log.error("保存部分AI响应失败", ex);
                }
            }
        };

        aiChatService.chatStreamWithContinuation(message, null, new StreamingChatResponseHandler()
        {
            @Override
            public void onPartialResponse(String partialResponse)
            {
                fullResponse.append(partialResponse);
                SseHelper.writeData(asyncContext, partialResponse);
            }

            @Override
            public void onCompleteResponse(ChatResponse completeResponse)
            {
                if (replySaved.compareAndSet(false, true))
                {
                    long costTime = System.currentTimeMillis() - startTime;
                    try {
                        aiChatSessionService.saveAiReply(actualConvId, fullResponse.toString(),
                                null, "DeepSeek V3", 0, (int) costTime);
                    } catch (Exception ex) {
                        log.error("保存AI响应失败", ex);
                    }
                }
                SseHelper.writeEvent(asyncContext, "done", "[DONE]");
                SseHelper.complete(asyncContext);
            }

            @Override
            public void onError(Throwable error)
            {
                log.error("AI流式聊天错误", error);
                savePartialResponse.run();
                SseHelper.complete(asyncContext);
            }
        });
    }

    /**
     * Dify对话（带消息持久化，仿FastGPT惰性创建模式）
     * conversationId可为null/0，此时自动创建会话
     */
    @PostMapping("/dify/chat")
    @ResponseBody
    public AjaxResult difyChatWithPersistence(@RequestParam(value = "conversationId", required = false) Long conversationId,
                                               @RequestParam("content") String content)
    {
        try
        {
            long startTime = System.currentTimeMillis();
            Long userId = ShiroUtils.getUserId();

            // 惰性创建会话（仿FastGPT的upsert）
            AiChatConversation conversation = aiChatSessionService.ensureConversation(conversationId, userId, content);
            if (conversation == null)
            {
                return AjaxResult.error("创建会话失败");
            }
            Long actualConvId = conversation.getConversationId();

            // 验证用户归属
            if (!userId.equals(conversation.getUserId()))
            {
                return AjaxResult.error("会话不存在或无权访问");
            }

            // 保存用户消息
            AiChatMessage userMsg = aiChatSessionService.sendMessage(actualConvId, content);

            // 调用Dify API
            String user = ShiroUtils.getLoginName();
            Map<String, String> difyResult = difyApiClient.chatMessage(
                conversation.getAppId(), content, user, conversation.getDifyConversationId());

            long costTime = System.currentTimeMillis() - startTime;

            // 首次对话时保存Dify返回的conversationId
            String difyConvId = difyResult.get("conversationId");
            if (difyConvId != null && !difyConvId.isEmpty()
                && (conversation.getDifyConversationId() == null || conversation.getDifyConversationId().isEmpty()))
            {
                aiChatSessionService.updateDifyConversationId(actualConvId, difyConvId);
            }

            // 解析Dify响应
            String answer = difyResult.getOrDefault("answer", "");
            String messageId = difyResult.getOrDefault("messageId", "");
            String model = difyResult.getOrDefault("model", "");
            int tokens = 0;
            try { tokens = Integer.parseInt(difyResult.getOrDefault("totalTokens", "0")); } catch (Exception ignored) {}

            // 保存AI回复消息
            AiChatMessage aiMsg = aiChatSessionService
                .saveAiReply(actualConvId, answer, messageId, model, tokens, (int) costTime);

            // 返回结果（包含actualConvId，前端用于惰性创建后更新状态）
            Map<String, Object> result = new HashMap<>();
            result.put("answer", answer);
            result.put("conversationId", actualConvId);
            result.put("messageId", aiMsg.getMessageId());
            result.put("difyMessageId", messageId);
            result.put("model", model);
            result.put("tokens", tokens);
            result.put("costTime", costTime);
            return AjaxResult.success("操作成功", result);
        }
        catch (Exception e)
        {
            log.error("Dify对话失败", e);
            return AjaxResult.error("对话失败：" + e.getMessage());
        }
    }
}
