package com.ruoyi.web.controller.ai;

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
