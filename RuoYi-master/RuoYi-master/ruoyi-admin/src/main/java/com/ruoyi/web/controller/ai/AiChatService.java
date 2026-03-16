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
            你是一个资深Java架构师，精通RuoYi v4.8.2框架。请严格按照以下框架约束进行分析：

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

            ## 输出要求
            所有输出使用Markdown格式，表格对齐，中文标点。
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
     * 同步聊天（带自定义System Prompt）
     *
     * @param userMessage 用户消息
     * @param systemPrompt 系统提示词（为空则使用默认RuoYi规范）
     * @return AI完整回复
     */
    public String chat(String userMessage, String systemPrompt)
    {
        try
        {
            List<ChatMessage> messages = new ArrayList<>();
            messages.add(SystemMessage.from(systemPrompt != null ? systemPrompt : RUOYI_SYSTEM_PROMPT));
            messages.add(UserMessage.from(userMessage));

            ChatResponse response = chatLanguageModel.chat(messages);
            return response.aiMessage().text();
        }
        catch (Exception e)
        {
            log.error("AI聊天请求失败", e);
            return "AI服务暂时不可用，请检查API Key配置。错误信息：" + e.getMessage();
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
     * 需求分析 — 提取5类关键信息
     *
     * @param documentContent 需求文档内容
     * @return AI分析结果（Markdown格式）
     */
    public String analyzeRequirement(String documentContent)
    {
        String prompt = """
                请分析以下需求文档，按照以下5个维度提取关键信息：

                1. 功能点清单：列出所有需要开发的功能，每个功能标注类型（CRUD/树形/主子表/统计）
                2. 角色定义：列出所有用户角色，每个角色能做什么、不能做什么
                3. 业务流程：画出核心业务流程（用文字箭头描述），标注每一步涉及的数据变更
                4. 数据实体和字段：列出每个数据实体的所有字段，包括字段名、类型、是否必填、是否查询条件、是否字典
                5. 非功能需求：性能要求、安全要求、兼容性要求等

                【需求文档内容】
                """ + documentContent;
        return chat(prompt);
    }

    /**
     * RuoYi适配分析
     *
     * @param analysisResult 1.3步骤的分析结果
     * @return 适配分析报告（Markdown格式）
     */
    public String ruoyiAdaptAnalysis(String analysisResult)
    {
        String prompt = """
                基于以下需求分析结果，请逐条回答以下7个RuoYi适配问题：

                1. 功能重叠检查：上述功能是否与RuoYi内置的18个功能有重叠？如果有，说明如何复用。
                2. 角色复用：需求中的角色能否映射到RuoYi已有的角色？是否需要新建角色？
                3. 字典字段识别：哪些字段的值是固定选项？应该使用sys_dict_type + sys_dict_data管理。
                4. 数据权限需求：是否需要按部门隔离数据？哪些实体需要@DataScope注解？
                5. 树形结构判断：是否有实体需要父子层级关系？需要继承TreeEntity。
                6. 主子表关系：是否有一对多关系？需要使用RuoYi的主子表模板。
                7. 特殊能力需求：是否需要定时任务、文件上传、Excel导入导出？

                【需求分析结果】
                """ + analysisResult;
        return chat(prompt);
    }

    /**
     * 完整性评估
     *
     * @param analysisResult 前面步骤的分析结果
     * @return 完整性评估报告（Markdown格式）
     */
    public String completenessEvaluation(String analysisResult)
    {
        String prompt = """
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
                """ + analysisResult;
        return chat(prompt);
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
        String prompt = """
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
                """ + allAnalysisResults;
        return chat(prompt);
    }

    /**
     * 获取默认System Prompt
     */
    public String getDefaultSystemPrompt()
    {
        return RUOYI_SYSTEM_PROMPT;
    }
}
