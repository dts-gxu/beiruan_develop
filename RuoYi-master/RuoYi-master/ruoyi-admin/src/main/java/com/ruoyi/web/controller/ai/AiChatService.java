package com.ruoyi.web.controller.ai;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.output.FinishReason;

/**
 * AI聊天服务
 * 
 * 提供同步和流式两种对话方式，支持需求分析等场景
 * 
 * @author ruoyi
 */
@Service
public class AiChatService
{
    private static final Logger log = LoggerFactory.getLogger(AiChatService.class);

    @Autowired
    private ChatLanguageModel chatLanguageModel;

    @Autowired
    private StreamingChatLanguageModel streamingChatLanguageModel;

    /** RuoYi框架规范System Prompt */
    private static final String RUOYI_SYSTEM_PROMPT = """
            你精通RuoYi v4.8.2框架，请严格按照以下框架约束进行分析。

            【重要】输出规则：
            - 直接输出分析结果，不要自我介绍，不要说"好的"、"作为..."等开场白
            - 使用纯Markdown格式，禁止使用任何HTML标签（如<br>、<b>、<hr>等）
            - 换行请直接换行，不要用<br>标签
            - 表格使用Markdown表格语法，表格内容不要换行
            - 中文标点，表格对齐

            ## 技术栈
            - Spring Boot 4.0.3 + Apache Shiro 2.1.0 + MyBatis + Thymeleaf + Bootstrap 3.3.7
            - 前后端不分离，服务端渲染HTML
            - JDK 17+，MySQL 5.7+/8.0+

            ## 编码红线
            - 不使用Lombok，手写getter/setter
            - 不使用@RestController，使用@Controller + @ResponseBody
            - Domain必须继承BaseEntity或TreeEntity
            - Controller必须继承BaseController
            - 每个方法必须有@RequiresPermissions权限注解
            - 增删改必须有@Log日志注解

            ## 已有能力（不要重复开发）
            - 用户/角色/部门/菜单/字典/参数/日志管理已内置
            - 数据权限通过@DataScope注解实现
            - Excel导入导出通过@Excel注解 + ExcelUtil实现
            - 文件上传通过CommonController实现
            - 定时任务通过ruoyi-quartz模块实现

            ## BaseEntity已有字段（新表不需要再定义）
            create_by, create_time, update_by, update_time, remark
            """;

    /** 通用聊天System Prompt（普通对话场景） */
    private static final String GENERAL_CHAT_PROMPT = """
            你是一个专业的AI助手。

            【输出规则】
            - 直接回答问题，不要自我介绍，不要说"好的"、"作为..."等开场白
            - 使用纯Markdown格式，禁止使用任何HTML标签（如<br>、<b>、<hr>等）
            - 换行请直接换行，不要用<br>标签
            - 表格使用Markdown表格语法
            - 中文回复，简洁专业
            """;

    /**
     * 同步聊天（等待完整响应）
     *
     * @param userMessage 用户消息
     * @return AI完整回复
     */
    public String chat(String userMessage)
    {
        return chat(userMessage, null);
    }

    /**
     * 通用聊天（使用轻量级提示词，适用于普通对话）
     */
    public String chatGeneral(String userMessage)
    {
        return chat(userMessage, GENERAL_CHAT_PROMPT);
    }

    /** 知识库RAG System Prompt - 前缀（基本规则 + 知识库内容开始标记） */
    private static final String RAG_SYSTEM_PROMPT_PREFIX = """
            你是一个专业的AI助手。请根据下方知识库内容回答用户问题。
            优先使用知识库内容，直接回答，Markdown格式，中文回复。

            【知识库内容】
            """;

    /** 知识库RAG System Prompt - 后缀（在知识库内容之后，离AI生成最近，最不可能被忽略） */
    private static final String RAG_SYSTEM_PROMPT_SUFFIX = """

            【引用标注规则 - 必须严格遵守】
            在回答中，每当你使用了上面某条知识库内容时，必须在该句末尾用markdown链接格式标注来源：[编号](CITE)
            - 编号对应知识库内容前的数字
            - 一句话可以有多个来源，连续写多个链接
            - 不要在末尾单独列参考文献

            示例输出：
            数据库表名应使用小写字母和下划线[2](CITE)。事务中SQL不超过5个[5](CITE)。主键用自增ID，高并发用分布式方案[2](CITE)[7](CITE)。
            """;

    /**
     * 带知识库上下文的同步聊天（RAG模式）
     * @param userMessage 用户消息
     * @param knowledgeContext 知识库检索结果
     * @return AI回复
     */
    public String chatWithKnowledge(String userMessage, String knowledgeContext)
    {
        // 使用字符串拼接代替String.format，避免知识库内容含%字符导致异常
        String ragPrompt = RAG_SYSTEM_PROMPT_PREFIX + knowledgeContext + RAG_SYSTEM_PROMPT_SUFFIX;
        log.info("RAG同步聊天: 知识库上下文长度={}字符, 用户消息长度={}字符", knowledgeContext.length(), userMessage.length());
        return chat(userMessage, ragPrompt);
    }

    /**
     * 带知识库上下文的流式聊天（RAG模式）
     * @param userMessage 用户消息
     * @param knowledgeContext 知识库检索结果
     * @param handler 流式响应处理器
     */
    public void chatStreamWithKnowledge(String userMessage, String knowledgeContext,
            StreamingChatResponseHandler handler)
    {
        // 使用字符串拼接代替String.format，避免知识库内容含%字符导致异常
        String ragPrompt = RAG_SYSTEM_PROMPT_PREFIX + knowledgeContext + RAG_SYSTEM_PROMPT_SUFFIX;
        log.info("RAG流式聊天: 知识库上下文长度={}字符, 用户消息长度={}字符", knowledgeContext.length(), userMessage.length());
        chatStream(userMessage, ragPrompt, handler);
    }

    /** 自动续写最大轮数 */
    private static final int MAX_CONTINUATION_ROUNDS = 3;

    /**
     * 同步聊天（带自定义System Prompt + 自动续写）
     * 当AI响应因maxTokens被截断（finishReason=LENGTH）时，自动追加"请继续"获取剩余内容
     *
     * @param userMessage 用户消息
     * @param systemPrompt 系统提示词（为空则使用默认RuoYi规范）
     * @return AI完整回复（多轮续写自动拼接）
     */
    public String chat(String userMessage, String systemPrompt)
    {
        try
        {
            List<ChatMessage> messages = new ArrayList<>();
            messages.add(SystemMessage.from(systemPrompt != null ? systemPrompt : RUOYI_SYSTEM_PROMPT));
            messages.add(UserMessage.from(userMessage));

            StringBuilder fullResponse = new StringBuilder();

            for (int round = 0; round <= MAX_CONTINUATION_ROUNDS; round++)
            {
                ChatResponse response = chatLanguageModel.chat(messages);
                String text = response.aiMessage().text();
                fullResponse.append(text);

                // 检查是否因token限制被截断
                FinishReason reason = response.finishReason();
                if (reason == FinishReason.LENGTH && round < MAX_CONTINUATION_ROUNDS)
                {
                    log.info("AI响应被截断（第{}轮），自动续写...", round + 1);
                    // 将AI的回复加入上下文，再追加"请继续"
                    messages.add(response.aiMessage());
                    messages.add(UserMessage.from("请继续上面未完成的内容，从截断处无缝衔接，不要重复已有内容"));
                }
                else
                {
                    // 正常结束（STOP）或已达最大续写轮数
                    break;
                }
            }

            return fullResponse.toString();
        }
        catch (Exception e)
        {
            log.error("AI聊天请求失败", e);
            throw new RuntimeException("AI服务暂时不可用：" + e.getMessage(), e);
        }
    }

    /**
     * 带图片的聊天（视觉识别）
     *
     * @param userMessage 用户文字提示
     * @param base64Image 图片的Base64编码
     * @param mimeType 图片MIME类型（如image/png）
     * @return AI回复
     */
    public String chatWithImage(String userMessage, String base64Image, String mimeType)
    {
        try
        {
            List<ChatMessage> messages = new ArrayList<>();
            messages.add(SystemMessage.from("你是一个专业的OCR文字识别助手。请准确识别图片中的所有文字内容。"));

            List<Content> contents = new ArrayList<>();
            contents.add(TextContent.from(userMessage));
            contents.add(ImageContent.from(base64Image, mimeType));
            messages.add(UserMessage.from(contents));

            ChatResponse response = chatLanguageModel.chat(messages);
            return response.aiMessage().text();
        }
        catch (Exception e)
        {
            log.error("AI图片识别请求失败", e);
            throw new RuntimeException("图片识别失败：" + e.getMessage(), e);
        }
    }

    /**
     * 通用流式聊天（使用轻量级提示词，适用于普通对话）
     */
    public void chatStreamGeneral(String userMessage, StreamingChatResponseHandler handler)
    {
        chatStream(userMessage, GENERAL_CHAT_PROMPT, handler);
    }

    /**
     * 流式聊天（SSE逐字输出）
     *
     * @param userMessage 用户消息
     * @param systemPrompt 系统提示词（为空则使用默认RuoYi规范）
     * @param handler 流式响应处理器
     */
    public void chatStream(String userMessage, String systemPrompt, StreamingChatResponseHandler handler)
    {
        try
        {
            List<ChatMessage> messages = new ArrayList<>();
            messages.add(SystemMessage.from(systemPrompt != null ? systemPrompt : RUOYI_SYSTEM_PROMPT));
            messages.add(UserMessage.from(userMessage));

            streamingChatLanguageModel.chat(messages, handler);
        }
        catch (Exception e)
        {
            log.error("AI流式聊天请求失败", e);
            handler.onError(e);
        }
    }

    /**
     * 流式聊天 + 自动续写（检测finishReason=LENGTH时自动链式继续流式输出）
     * 前端可逐字看到完整响应，截断时无缝衔接下一轮流式
     */
    public void chatStreamWithContinuation(String userMessage, String systemPrompt,
            StreamingChatResponseHandler outerHandler)
    {
        try
        {
            List<ChatMessage> messages = new ArrayList<>();
            messages.add(SystemMessage.from(systemPrompt != null ? systemPrompt : RUOYI_SYSTEM_PROMPT));
            messages.add(UserMessage.from(userMessage));
            streamContinuationRound(messages, 0, outerHandler);
        }
        catch (Exception e)
        {
            log.error("流式续写聊天请求失败", e);
            outerHandler.onError(e);
        }
    }

    /**
     * 递归链式流式续写：每轮结束后检测是否截断，截断则追加"请继续"开启下一轮
     */
    private void streamContinuationRound(List<ChatMessage> messages, int round,
            StreamingChatResponseHandler outerHandler)
    {
        StringBuilder roundText = new StringBuilder();

        streamingChatLanguageModel.chat(messages, new StreamingChatResponseHandler()
        {
            @Override
            public void onPartialResponse(String partialResponse)
            {
                roundText.append(partialResponse);
                outerHandler.onPartialResponse(partialResponse);
            }

            @Override
            public void onCompleteResponse(ChatResponse response)
            {
                FinishReason reason = response.finishReason();
                if (reason == FinishReason.LENGTH && round < MAX_CONTINUATION_ROUNDS)
                {
                    log.info("流式响应被截断（第{}轮），自动续写...", round + 1);
                    messages.add(AiMessage.from(roundText.toString()));
                    messages.add(UserMessage.from("请继续上面未完成的内容，从截断处无缝衔接，不要重复已有内容"));
                    streamContinuationRound(messages, round + 1, outerHandler);
                }
                else
                {
                    outerHandler.onCompleteResponse(response);
                }
            }

            @Override
            public void onError(Throwable error)
            {
                outerHandler.onError(error);
            }
        });
    }

    /**
     * 构建Pipeline步骤的Prompt（供流式SSE端点复用）
     */
    public String buildPipelinePrompt(String step, String data)
    {
        switch (step)
        {
            case "analyze":
                return """
                    请分析以下需求文档，按照以下5个维度提取关键信息：

                    1. 功能点清单：列出所有需要开发的功能，每个功能标注类型（CRUD/树形/主子表/统计）
                    2. 角色定义：列出所有用户角色，每个角色能做什么、不能做什么
                    3. 业务流程：画出核心业务流程（用文字箭头描述），标注每一步涉及的数据变更
                    4. 数据实体和字段：列出每个数据实体的所有字段，包括字段名、类型、是否必填、是否查询条件、是否字典
                    5. 非功能需求：性能要求、安全要求、兼容性要求等

                    【需求文档内容】
                    """ + data;
            case "adapt":
                return """
                    基于以下需求分析结果，请逐条回答以下7个RuoYi适配问题：

                    1. 功能重叠检查：上述功能是否与RuoYi内置的18个功能有重叠？如果有，说明如何复用。
                    2. 角色复用：需求中的角色能否映射到RuoYi已有的角色？是否需要新建角色？
                    3. 字典字段识别：哪些字段的值是固定选项？应该使用sys_dict_type + sys_dict_data管理。
                    4. 数据权限需求：是否需要按部门隔离数据？哪些实体需要@DataScope注解？
                    5. 树形结构判断：是否有实体需要父子层级关系？需要继承TreeEntity。
                    6. 主子表关系：是否有一对多关系？需要使用RuoYi的主子表模板。
                    7. 特殊能力需求：是否需要定时任务、文件上传、Excel导入导出？

                    【需求分析结果】
                    """ + data;
            case "evaluate":
                return """
                    请对以下需求进行完整性评估，逐条检查以下10个维度，每个维度标注 ✅完整 / ⚠️部分缺失 / ❌严重缺失：

                    1. 角色与权限：每个角色的权限边界是否清晰？
                    2. 业务流程：正常流程是否完整？异常流程是否考虑？
                    3. 数据字段：每个字段是否有类型、长度、必填、默认值？
                    4. UI交互：列表搜索条件是否明确？表单验证规则是否明确？
                    5. 关联关系：实体之间的关系是否明确？级联操作是否定义？
                    6. 字典枚举：所有状态/类型字段是否定义了完整的枚举值？
                    7. 安全性：是否有敏感数据需要脱敏？
                    8. 性能：数据量预期？是否需要索引？
                    9. 兼容性：浏览器要求？移动端是否需要？
                    10. 可扩展性：未来可能的扩展方向？

                    最后给出总结和建议。

                    【需求分析结果】
                    """ + data;
            case "report":
                return """
                    请根据以下所有需求分析结果，生成一份正式的《需求完整性报告》，报告格式如下：

                    # 需求完整性报告

                    ## 一、项目概述
                    简要描述项目名称、业务背景、主要目标。

                    ## 二、功能需求汇总
                    列出所有功能模块及其子功能，以表格形式展示：
                    | 序号 | 功能模块 | 子功能 | 功能类型 | 优先级 | 备注 |

                    ## 三、角色与权限矩阵
                    以表格形式展示各角色对各功能的权限：
                    | 角色 | 功能1 | 功能2 | ... |

                    ## 四、数据实体设计
                    列出每个数据实体的完整字段清单：
                    | 字段名 | 中文名 | 类型 | 长度 | 必填 | 查询 | 字典 | 备注 |

                    ## 五、RuoYi框架适配方案
                    说明哪些功能可以复用RuoYi已有能力，哪些需要定制开发。

                    ## 六、完整性评估结果
                    以表格形式汇总10个维度的评估结果：
                    | 维度 | 评估结果 | 说明 |

                    ## 七、待确认问题清单
                    列出需要与需求方确认的问题。

                    ## 八、风险评估
                    列出潜在的技术风险和业务风险。

                    ## 九、总结与建议
                    给出整体评价和下一步建议。

                    请确保报告内容详尽、专业、格式规范。

                    【分析结果汇总】
                    """ + data;
            default:
                return data;
        }
    }

    /**
     * 需求分析 — 提取5类关键信息
     *
     * @param documentContent 需求文档内容
     * @return AI分析结果（Markdown格式）
     */
    public String analyzeRequirement(String documentContent)
    {
        return chat(buildPipelinePrompt("analyze", documentContent));
    }

    /**
     * RuoYi适配分析
     *
     * @param analysisResult 1.3步骤的分析结果
     * @return 适配分析报告（Markdown格式）
     */
    public String ruoyiAdaptAnalysis(String analysisResult)
    {
        return chat(buildPipelinePrompt("adapt", analysisResult));
    }

    /**
     * 完整性评估
     *
     * @param analysisResult 前面步骤的分析结果
     * @return 完整性评估报告（Markdown格式）
     */
    public String completenessEvaluation(String analysisResult)
    {
        return chat(buildPipelinePrompt("evaluate", analysisResult));
    }

    /**
     * 生成结构化需求JSON
     *
     * @param analysisResult 前面步骤的全部分析结果
     * @return 结构化JSON字符串
     */
    public String generateRequirementJson(String analysisResult)
    {
        String prompt = """
                请根据以下需求分析，生成结构化需求JSON，必须严格遵循以下Schema：

                {
                  "module": "模块英文名（小写）",
                  "moduleName": "模块中文名",
                  "packageName": "com.ruoyi.{module}",
                  "author": "ruoyi",
                  "tablePrefix": "业务表前缀_",
                  "entities": [
                    {
                      "tableName": "完整表名",
                      "className": "Java类名（大驼峰）",
                      "tableComment": "表中文说明",
                      "tplCategory": "crud/tree/sub",
                      "fields": [
                        {
                          "columnName": "数据库列名",
                          "columnComment": "字段中文说明",
                          "columnType": "数据库类型",
                          "javaType": "Java类型",
                          "javaField": "Java字段名",
                          "isPk": "是否主键",
                          "isIncrement": "是否自增",
                          "isRequired": "是否必填",
                          "isInsert": "是否插入字段",
                          "isEdit": "是否编辑字段",
                          "isList": "是否列表显示",
                          "isQuery": "是否查询条件",
                          "queryType": "查询方式（EQ/LIKE/BETWEEN等）",
                          "htmlType": "控件类型",
                          "dictType": "字典类型"
                        }
                      ]
                    }
                  ],
                  "dictionaries": [
                    {
                      "dictType": "字典类型编码",
                      "dictName": "字典类型名称",
                      "items": [
                        {"label": "显示标签", "value": "存储值", "listClass": "样式", "isDefault": "Y/N"}
                      ]
                    }
                  ],
                  "menus": {
                    "parentName": "一级目录名称",
                    "parentIcon": "fa fa-xxx",
                    "children": [
                      {
                        "menuName": "菜单名称",
                        "url": "访问URL",
                        "permsPrefix": "权限前缀",
                        "icon": "fa fa-xxx",
                        "buttons": ["list", "add", "edit", "remove", "export"]
                      }
                    ]
                  }
                }

                只输出JSON，不要其他文字。

                【需求分析结果】
                """ + analysisResult;
        return chat(prompt);
    }

    /**
     * 生成完整性报告（步骤1.6）
     *
     * @param allAnalysisResults 前面所有步骤的分析结果
     * @return 完整的需求完整性报告（Markdown格式）
     */
    public String generateCompletenessReport(String allAnalysisResults)
    {
        return chat(buildPipelinePrompt("report", allAnalysisResults));
    }

    /**
     * 获取默认System Prompt
     */
    public String getDefaultSystemPrompt()
    {
        return RUOYI_SYSTEM_PROMPT;
    }
}
