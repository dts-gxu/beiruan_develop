package com.ruoyi.web.controller.ai;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
                                       @RequestParam("data") String data)
    {
        String taskId = java.util.UUID.randomUUID().toString().substring(0, 12);
        pendingPipelineTasks.put(taskId, new String[]{step, data});
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
        String prompt = aiChatService.buildPipelinePrompt(step, data);
        if (prompt == null || prompt.isEmpty())
        {
            SseHelper.writeEvent(response, "error", "无效的分析步骤");
            return;
        }

        AsyncContext asyncContext = SseHelper.startAsync(request, 300000);
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
