package com.ruoyi.web.controller.ai;

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
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.utils.ShiroUtils;
import com.ruoyi.system.domain.AiChatConversation;
import com.ruoyi.system.domain.AiChatMessage;
import com.ruoyi.system.domain.AiDifyApp;
import com.ruoyi.system.service.IAiChatSessionService;
import com.ruoyi.system.service.IAiDifyAppService;
import com.ruoyi.system.service.impl.AiChatSessionServiceImpl;
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
    private CodeGenerationService codeGenerationService;

    @Autowired
    private TestGenerationService testGenerationService;

    @Autowired
    private CodeReviewService codeReviewService;

    @Autowired
    private DifyApiClient difyApiClient;

    @Autowired
    private IAiDifyAppService aiDifyAppService;

    @Autowired
    private IAiChatSessionService aiChatSessionService;

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
     * AI流式聊天接口（SSE）
     */
    @RequiresPermissions("ai:assistant:chat")
    @GetMapping("/chat/stream")
    public SseEmitter chatStream(@RequestParam("message") String message)
    {
        SseEmitter emitter = new SseEmitter(180000L);

        aiChatService.chatStream(message, null, new StreamingChatResponseHandler()
        {
            @Override
            public void onPartialResponse(String partialResponse)
            {
                try
                {
                    emitter.send(SseEmitter.event().data(partialResponse));
                }
                catch (Exception e)
                {
                    log.error("SSE发送失败", e);
                    emitter.completeWithError(e);
                }
            }

            @Override
            public void onCompleteResponse(ChatResponse completeResponse)
            {
                try
                {
                    emitter.send(SseEmitter.event().name("done").data("[DONE]"));
                    emitter.complete();
                }
                catch (Exception e)
                {
                    log.error("SSE完成发送失败", e);
                }
            }

            @Override
            public void onError(Throwable error)
            {
                log.error("AI流式聊天错误", error);
                emitter.completeWithError(error);
            }
        });

        return emitter;
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
     * 生成结构化需求JSON（步骤1.7）
     */
    @RequiresPermissions("ai:assistant:analyze")
    @PostMapping("/generateJson")
    @ResponseBody
    public AjaxResult generateRequirementJson(@RequestParam("analysisResult") String analysisResult)
    {
        try
        {
            String result = aiChatService.generateRequirementJson(analysisResult);
            return AjaxResult.success("JSON生成完成", result);
        }
        catch (Exception e)
        {
            log.error("JSON生成失败", e);
            return AjaxResult.error("JSON生成失败：" + e.getMessage());
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

    // ====================== 第四步：代码生成 ======================

    /**
     * AI代码生成 — 根据结构化JSON生成全套RuoYi代码（第四步）
     */
    @RequiresPermissions("ai:assistant:generate")
    @PostMapping("/generate/code")
    @ResponseBody
    public AjaxResult generateCode(@RequestParam("requirementJson") String requirementJson)
    {
        try
        {
            java.util.Map<String, String> files = codeGenerationService.generateFullCode(requirementJson);
            java.util.Map<String, Object> result = new java.util.HashMap<>();
            result.put("files", files);
            result.put("fileCount", files.size());
            result.put("markdown", codeGenerationService.formatAsMarkdown(files));
            return AjaxResult.success("代码生成完成", result);
        }
        catch (Exception e)
        {
            log.error("代码生成失败", e);
            return AjaxResult.error("代码生成失败：" + e.getMessage());
        }
    }

    /**
     * 单独生成SQL脚本
     */
    @RequiresPermissions("ai:assistant:generate")
    @PostMapping("/generate/sql")
    @ResponseBody
    public AjaxResult generateSql(@RequestParam("requirementJson") String requirementJson)
    {
        try
        {
            String result = codeGenerationService.generateSql(requirementJson);
            return AjaxResult.success("SQL生成完成", result);
        }
        catch (Exception e)
        {
            log.error("SQL生成失败", e);
            return AjaxResult.error("SQL生成失败：" + e.getMessage());
        }
    }

    /**
     * 单独生成Domain实体类
     */
    @RequiresPermissions("ai:assistant:generate")
    @PostMapping("/generate/domain")
    @ResponseBody
    public AjaxResult generateDomain(@RequestParam("requirementJson") String requirementJson)
    {
        try
        {
            String result = codeGenerationService.generateDomain(requirementJson);
            return AjaxResult.success("Domain生成完成", result);
        }
        catch (Exception e)
        {
            log.error("Domain生成失败", e);
            return AjaxResult.error("Domain生成失败：" + e.getMessage());
        }
    }

    /**
     * 单独生成Mapper
     */
    @RequiresPermissions("ai:assistant:generate")
    @PostMapping("/generate/mapper")
    @ResponseBody
    public AjaxResult generateMapper(@RequestParam("requirementJson") String requirementJson)
    {
        try
        {
            String result = codeGenerationService.generateMapper(requirementJson);
            return AjaxResult.success("Mapper生成完成", result);
        }
        catch (Exception e)
        {
            log.error("Mapper生成失败", e);
            return AjaxResult.error("Mapper生成失败：" + e.getMessage());
        }
    }

    /**
     * 单独生成Service
     */
    @RequiresPermissions("ai:assistant:generate")
    @PostMapping("/generate/service")
    @ResponseBody
    public AjaxResult generateService(@RequestParam("requirementJson") String requirementJson)
    {
        try
        {
            String result = codeGenerationService.generateService(requirementJson);
            return AjaxResult.success("Service生成完成", result);
        }
        catch (Exception e)
        {
            log.error("Service生成失败", e);
            return AjaxResult.error("Service生成失败：" + e.getMessage());
        }
    }

    /**
     * 单独生成Controller
     */
    @RequiresPermissions("ai:assistant:generate")
    @PostMapping("/generate/controller")
    @ResponseBody
    public AjaxResult generateController(@RequestParam("requirementJson") String requirementJson)
    {
        try
        {
            String result = codeGenerationService.generateController(requirementJson);
            return AjaxResult.success("Controller生成完成", result);
        }
        catch (Exception e)
        {
            log.error("Controller生成失败", e);
            return AjaxResult.error("Controller生成失败：" + e.getMessage());
        }
    }

    /**
     * 单独生成HTML页面
     */
    @RequiresPermissions("ai:assistant:generate")
    @PostMapping("/generate/html")
    @ResponseBody
    public AjaxResult generateHtml(@RequestParam("requirementJson") String requirementJson)
    {
        try
        {
            String result = codeGenerationService.generateHtml(requirementJson);
            return AjaxResult.success("HTML页面生成完成", result);
        }
        catch (Exception e)
        {
            log.error("HTML生成失败", e);
            return AjaxResult.error("HTML生成失败：" + e.getMessage());
        }
    }

    // ====================== 第五步：测试生成 ======================

    /**
     * AI生成测试用例（第五步）
     */
    @RequiresPermissions("ai:assistant:generate")
    @PostMapping("/generate/test")
    @ResponseBody
    public AjaxResult generateTests(@RequestParam("generatedCode") String generatedCode,
                                     @RequestParam("requirementJson") String requirementJson)
    {
        try
        {
            java.util.Map<String, String> tests = testGenerationService.generateAllTests(generatedCode, requirementJson);
            java.util.Map<String, Object> result = new java.util.HashMap<>();
            result.put("files", tests);
            result.put("fileCount", tests.size());
            return AjaxResult.success("测试用例生成完成", result);
        }
        catch (Exception e)
        {
            log.error("测试生成失败", e);
            return AjaxResult.error("测试生成失败：" + e.getMessage());
        }
    }

    // ====================== 第六步：代码评审 ======================

    /**
     * AI代码评审（第六步）
     */
    @RequiresPermissions("ai:assistant:generate")
    @PostMapping("/review/code")
    @ResponseBody
    public AjaxResult reviewCode(@RequestParam("code") String code)
    {
        try
        {
            String report = codeReviewService.reviewCode(code);
            return AjaxResult.success("代码评审完成", report);
        }
        catch (Exception e)
        {
            log.error("代码评审失败", e);
            return AjaxResult.error("代码评审失败：" + e.getMessage());
        }
    }

    /**
     * 根据评审结果自动修复代码
     */
    @RequiresPermissions("ai:assistant:generate")
    @PostMapping("/review/fix")
    @ResponseBody
    public AjaxResult fixCode(@RequestParam("code") String code,
                              @RequestParam("reviewReport") String reviewReport)
    {
        try
        {
            String fixedCode = codeReviewService.fixCode(code, reviewReport);
            return AjaxResult.success("代码修复完成", fixedCode);
        }
        catch (Exception e)
        {
            log.error("代码修复失败", e);
            return AjaxResult.error("代码修复失败：" + e.getMessage());
        }
    }

    // ====================== Dify深度集成：会话管理 ======================

    /**
     * 获取可用的Dify应用列表（供前端下拉选择）
     */
    @RequiresPermissions("ai:assistant:chat")
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
    @RequiresPermissions("ai:assistant:chat")
    @PostMapping("/conversation/create")
    @ResponseBody
    public AjaxResult createConversation(@RequestParam("appId") Long appId,
                                          @RequestParam(value = "title", required = false) String title,
                                          @RequestParam(value = "conversationType", required = false) String conversationType)
    {
        try
        {
            Long userId = ShiroUtils.getUserId();
            AiChatConversation conversation = aiChatSessionService.createConversation(appId, userId, title, conversationType);
            return AjaxResult.success("创建成功", conversation);
        }
        catch (Exception e)
        {
            log.error("创建会话失败", e);
            return AjaxResult.error("创建会话失败：" + e.getMessage());
        }
    }

    /**
     * 获取当前用户的会话列表
     */
    @RequiresPermissions("ai:assistant:chat")
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
     * 获取会话的历史消息
     */
    @RequiresPermissions("ai:assistant:chat")
    @PostMapping("/conversation/messages")
    @ResponseBody
    public AjaxResult getConversationMessages(@RequestParam("conversationId") Long conversationId)
    {
        try
        {
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
     * 删除会话
     */
    @RequiresPermissions("ai:assistant:chat")
    @PostMapping("/conversation/delete")
    @ResponseBody
    public AjaxResult deleteConversation(@RequestParam("conversationId") Long conversationId)
    {
        try
        {
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
     * 修改会话标题
     */
    @RequiresPermissions("ai:assistant:chat")
    @PostMapping("/conversation/rename")
    @ResponseBody
    public AjaxResult renameConversation(@RequestParam("conversationId") Long conversationId,
                                          @RequestParam("title") String title)
    {
        try
        {
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
     * 通过Dify发送消息（核心接口）
     * 流程：保存用户消息 → 调用Dify Chat API → 保存AI回复 → 更新会话
     */
    @RequiresPermissions("ai:assistant:chat")
    @PostMapping("/dify/chat")
    @ResponseBody
    public AjaxResult difyChatWithPersistence(@RequestParam("conversationId") Long conversationId,
                                               @RequestParam("content") String content)
    {
        try
        {
            long startTime = System.currentTimeMillis();

            // 1. 查询会话信息
            AiChatConversation conversation = aiChatSessionService.selectConversationById(conversationId);
            if (conversation == null)
            {
                return AjaxResult.error("会话不存在");
            }

            // 2. 保存用户消息
            AiChatMessage userMsg = aiChatSessionService.sendMessage(conversationId, content);

            // 3. 调用Dify Chat API
            String user = ShiroUtils.getLoginName();
            Map<String, String> difyResult = difyApiClient.chatMessage(
                conversation.getAppId(), content, user, conversation.getDifyConversationId());

            long costTime = System.currentTimeMillis() - startTime;

            // 4. 更新Dify会话ID（首次对话时Dify返回新的conversationId）
            String difyConvId = difyResult.get("conversationId");
            if (difyConvId != null && !difyConvId.isEmpty()
                && (conversation.getDifyConversationId() == null || conversation.getDifyConversationId().isEmpty()))
            {
                ((AiChatSessionServiceImpl) aiChatSessionService).updateDifyConversationId(conversationId, difyConvId);
            }

            // 5. 保存AI回复消息
            String answer = difyResult.getOrDefault("answer", "");
            String messageId = difyResult.getOrDefault("messageId", "");
            String model = difyResult.getOrDefault("model", "");
            int tokens = 0;
            try { tokens = Integer.parseInt(difyResult.getOrDefault("totalTokens", "0")); } catch (Exception ignored) {}

            AiChatMessage aiMsg = ((AiChatSessionServiceImpl) aiChatSessionService)
                .saveAiReply(conversationId, answer, messageId, model, tokens, (int) costTime);

            // 6. 返回结果
            Map<String, Object> result = new HashMap<>();
            result.put("answer", answer);
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

    /**
     * 上传需求文件（支持PDF/Word/Excel/图片/TXT）
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
            String text = fileParseService.parseFile(file);
            if (text == null || text.trim().isEmpty())
            {
                return AjaxResult.error("文件内容为空，无法提取文本");
            }
            log.info("文件上传解析成功：{}，提取文本长度：{}", file.getOriginalFilename(), text.length());
            return AjaxResult.success("文件解析成功", text);
        }
        catch (Exception e)
        {
            log.error("文件上传解析失败", e);
            return AjaxResult.error("文件解析失败：" + e.getMessage());
        }
    }
}
