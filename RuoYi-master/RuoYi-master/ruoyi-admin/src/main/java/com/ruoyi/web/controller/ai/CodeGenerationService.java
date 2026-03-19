package com.ruoyi.web.controller.ai;

import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * AI代码生成服务（第四步）
 * 
 * 根据结构化需求JSON，调用AI生成完整的RuoYi规范代码：
 * Domain → Mapper → MapperXML → Service → ServiceImpl → Controller → HTML → SQL
 * 
 * @author ruoyi
 */
@Service
public class CodeGenerationService
{
    private static final Logger log = LoggerFactory.getLogger(CodeGenerationService.class);

    @Autowired
    private AiChatService aiChatService;

    /** 代码生成专用System Prompt */
    private static final String CODE_GEN_SYSTEM_PROMPT = """
            你是一个RuoYi v4.8.2框架代码生成专家。请严格按照以下规范生成代码：

            【框架版本】
            - RuoYi v4.8.2, Spring Boot 4.0.3, JDK 17+
            - MyBatis + Thymeleaf + Bootstrap 3.3.7 + Apache Shiro 2.1.0

            【编码红线】
            - Domain必须继承BaseEntity，不使用Lombok，手写getter/setter
            - Controller必须继承BaseController，使用@Controller（非@RestController）
            - 每个方法必须有@RequiresPermissions权限注解
            - 增删改必须有@Log日志注解
            - 列表查询调用startPage()开启分页
            - 使用@Excel注解支持导出
            - 使用ToStringBuilder的toString方法

            【BaseEntity已有字段（不要在Domain中重复定义）】
            searchValue, createBy, createTime, updateBy, updateTime, remark, params

            【输出要求】
            只输出代码，不输出解释。每个文件用以下格式分隔：
            === FILE: 文件路径 ===
            代码内容
            === END FILE ===
            """;

    /**
     * 根据结构化JSON生成全套代码
     *
     * @param requirementJson 结构化需求JSON（步骤1.7输出）
     * @return 包含所有生成文件的Map（文件路径 → 代码内容）
     */
    public Map<String, String> generateFullCode(String requirementJson)
    {
        Map<String, String> allFiles = new LinkedHashMap<>();

        try
        {
            // 分步生成，避免单次输出过长
            log.info("开始代码生成，需求JSON长度：{}", requirementJson.length());

            // 1. 生成SQL（DDL + 菜单 + 字典）
            String sqlCode = generateSql(requirementJson);
            parseFiles(sqlCode, allFiles);

            // 2. 生成Domain实体类
            String domainCode = generateDomain(requirementJson);
            parseFiles(domainCode, allFiles);

            // 3. 生成Mapper接口和XML
            String mapperCode = generateMapper(requirementJson);
            parseFiles(mapperCode, allFiles);

            // 4. 生成Service接口和实现
            String serviceCode = generateService(requirementJson);
            parseFiles(serviceCode, allFiles);

            // 5. 生成Controller
            String controllerCode = generateController(requirementJson);
            parseFiles(controllerCode, allFiles);

            // 6. 生成Thymeleaf HTML页面
            String htmlCode = generateHtml(requirementJson);
            parseFiles(htmlCode, allFiles);

            log.info("代码生成完成，共生成{}个文件", allFiles.size());
        }
        catch (Exception e)
        {
            log.error("代码生成失败", e);
            allFiles.put("ERROR", "代码生成失败：" + e.getMessage());
        }

        return allFiles;
    }

    /**
     * 生成SQL脚本（DDL + 菜单权限 + 字典数据）
     */
    public String generateSql(String requirementJson)
    {
        String prompt = """
                根据以下结构化需求JSON，生成完整的SQL脚本，包括：
                1. 建表DDL（含索引、外键、注释，必须包含create_by/create_time/update_by/update_time/remark字段）
                2. sys_menu菜单SQL（一级目录→二级菜单→按钮权限：查询/新增/修改/删除/导出）
                3. sys_dict_type和sys_dict_data字典SQL（如果有字典字段）
                4. 示例数据INSERT（至少3条）

                使用MySQL语法，表名用snake_case，主键用BIGINT自增。
                输出格式：
                === FILE: sql/create_table.sql ===
                DDL内容
                === END FILE ===
                === FILE: sql/menu.sql ===
                菜单SQL
                === END FILE ===
                === FILE: sql/dict.sql ===
                字典SQL
                === END FILE ===

                【需求JSON】
                """ + requirementJson;
        return aiChatService.chat(prompt, CODE_GEN_SYSTEM_PROMPT);
    }

    /**
     * 生成Domain实体类
     */
    public String generateDomain(String requirementJson)
    {
        String prompt = """
                根据以下结构化需求JSON，生成Domain实体类Java代码。

                规范要求：
                - 继承BaseEntity（已有createBy/createTime/updateBy/updateTime/remark，不要重复定义）
                - 不使用Lombok，手写getter/setter
                - 使用@Excel注解（name属性必填，日期字段加dateFormat和width）
                - 日期字段加@JsonFormat(pattern="yyyy-MM-dd", timezone="GMT+8")
                - 字典字段@Excel加readConverterExp
                - 使用ToStringBuilder的toString
                - 每个实体一个文件，包名com.ruoyi.{module}.domain

                输出格式：
                === FILE: java/domain/ClassName.java ===
                代码
                === END FILE ===

                【需求JSON】
                """ + requirementJson;
        return aiChatService.chat(prompt, CODE_GEN_SYSTEM_PROMPT);
    }

    /**
     * 生成Mapper接口和XML
     */
    public String generateMapper(String requirementJson)
    {
        String prompt = """
                根据以下结构化需求JSON，生成Mapper接口和MyBatis XML文件。

                Mapper接口规范：
                - 包名com.ruoyi.{module}.mapper
                - 标准CRUD方法：selectList/selectById/insert/update/deleteById/deleteByIds
                - 如有关联查询，添加额外方法

                Mapper XML规范：
                - 完整resultMap映射
                - sql片段定义select字段
                - selectList使用动态SQL（<if test>）
                - insert使用trim动态插入
                - update使用trim动态更新
                - 支持日期范围查询（params.beginTime/params.endTime）
                - 如有关联表，使用LEFT JOIN

                输出格式：
                === FILE: java/mapper/ClassNameMapper.java ===
                代码
                === END FILE ===
                === FILE: xml/ClassNameMapper.xml ===
                代码
                === END FILE ===

                【需求JSON】
                """ + requirementJson;
        return aiChatService.chat(prompt, CODE_GEN_SYSTEM_PROMPT);
    }

    /**
     * 生成Service接口和实现类
     */
    public String generateService(String requirementJson)
    {
        String prompt = """
                根据以下结构化需求JSON，生成Service接口和实现类。

                Service接口规范：
                - 包名com.ruoyi.{module}.service
                - 接口名I{ClassName}Service
                - 标准CRUD方法

                ServiceImpl规范：
                - 包名com.ruoyi.{module}.service.impl
                - @Service注解
                - @Autowired注入Mapper
                - insert方法设置createTime
                - update方法设置updateTime
                - deleteByIds使用Convert.toStrArray转换
                - 如有业务校验（如名称唯一性、删除前检查关联），请实现

                输出格式：
                === FILE: java/service/IClassNameService.java ===
                代码
                === END FILE ===
                === FILE: java/service/impl/ClassNameServiceImpl.java ===
                代码
                === END FILE ===

                【需求JSON】
                """ + requirementJson;
        return aiChatService.chat(prompt, CODE_GEN_SYSTEM_PROMPT);
    }

    /**
     * 生成Controller
     */
    public String generateController(String requirementJson)
    {
        String prompt = """
                根据以下结构化需求JSON，生成Controller类。

                Controller规范：
                - 继承BaseController
                - 包名com.ruoyi.web.controller.{module}
                - @Controller + @RequestMapping("/{module}/{business}")
                - prefix变量指向模板路径
                - @Autowired注入Service
                - 页面跳转方法返回String视图路径
                - JSON返回方法加@ResponseBody
                - 每个方法加@RequiresPermissions
                - 增删改加@Log注解
                - 列表查询用startPage() + getDataTable()
                - 导出用ExcelUtil
                - 新增/修改设置createBy/updateBy（用getLoginName()）
                - 编辑方法通过ModelMap传递数据
                - 如果实体有关联数据（如下拉选择），在add/edit方法中传递

                输出格式：
                === FILE: java/controller/ClassNameController.java ===
                代码
                === END FILE ===

                【需求JSON】
                """ + requirementJson;
        return aiChatService.chat(prompt, CODE_GEN_SYSTEM_PROMPT);
    }

    /**
     * 生成Thymeleaf HTML页面（列表页+新增页+编辑页）
     */
    public String generateHtml(String requirementJson)
    {
        String prompt = """
                根据以下结构化需求JSON，生成Thymeleaf HTML页面。

                每个实体需要3个页面：
                1. 列表页（{business}.html）：
                   - 搜索条件区域（根据isQuery字段）
                   - 字典字段用select + th:with获取字典数据
                   - 日期范围用time-input
                   - 工具栏按钮（添加/修改/删除/导出）+ shiro:hasPermission
                   - bootstrap-table表格初始化
                   - 字典字段用formatter + $.table.selectDictLabel翻译
                   - 操作列（编辑/删除按钮）

                2. 新增页（add.html）：
                   - 表单控件根据htmlType生成（input/textarea/select/radio/datetime）
                   - 字典字段用select + th:with
                   - 日期字段用laydate
                   - 必填字段加required
                   - 关联下拉（如果有外键关联）用select + th:each
                   - $.validate.form()表单验证
                   - submitHandler提交

                3. 编辑页（edit.html）：
                   - 与新增页类似，但数据回显
                   - 用th:value / th:field回显
                   - 字典字段用th:selected回显

                页面引用include :: header和include :: footer。

                输出格式：
                === FILE: html/{module}/{business}/{business}.html ===
                代码
                === END FILE ===
                === FILE: html/{module}/{business}/add.html ===
                代码
                === END FILE ===
                === FILE: html/{module}/{business}/edit.html ===
                代码
                === END FILE ===

                【需求JSON】
                """ + requirementJson;
        return aiChatService.chat(prompt, CODE_GEN_SYSTEM_PROMPT);
    }

    /**
     * 解析AI返回的文件内容，提取各文件
     *
     * @param aiOutput AI生成的包含文件标记的内容
     * @param files 输出Map
     */
    private void parseFiles(String aiOutput, Map<String, String> files)
    {
        if (aiOutput == null || aiOutput.isEmpty()) return;

        String[] parts = aiOutput.split("===\\s*FILE:\\s*");
        for (String part : parts)
        {
            if (part.trim().isEmpty()) continue;
            int endMarker = part.indexOf("=== END FILE ===");
            if (endMarker < 0) endMarker = part.length();

            String content = part.substring(0, endMarker);
            int firstNewline = content.indexOf('\n');
            if (firstNewline < 0) continue;

            String filePath = content.substring(0, firstNewline).replace("===", "").trim();
            String fileContent = content.substring(firstNewline + 1).trim();

            // 去除可能的代码块标记
            fileContent = fileContent.replaceAll("^```\\w*\\n?", "").replaceAll("\\n?```$", "").trim();

            if (!filePath.isEmpty() && !fileContent.isEmpty())
            {
                files.put(filePath, fileContent);
                log.debug("解析文件: {} ({}字符)", filePath, fileContent.length());
            }
        }
    }

    /**
     * 将生成的代码格式化为可读的Markdown输出
     *
     * @param files 文件Map
     * @return Markdown格式的代码展示
     */
    public String formatAsMarkdown(Map<String, String> files)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("## 代码生成结果\n\n");
        sb.append("共生成 **").append(files.size()).append("** 个文件：\n\n");

        int index = 1;
        for (Map.Entry<String, String> entry : files.entrySet())
        {
            String path = entry.getKey();
            String content = entry.getValue();
            String lang = guessLanguage(path);

            sb.append("### ").append(index++).append(". ").append(path).append("\n\n");
            sb.append("```").append(lang).append("\n");
            sb.append(content).append("\n");
            sb.append("```\n\n");
        }

        return sb.toString();
    }

    /**
     * 根据文件扩展名猜测语言
     */
    private String guessLanguage(String path)
    {
        if (path.endsWith(".java")) return "java";
        if (path.endsWith(".xml")) return "xml";
        if (path.endsWith(".html")) return "html";
        if (path.endsWith(".sql")) return "sql";
        if (path.endsWith(".js")) return "javascript";
        if (path.endsWith(".css")) return "css";
        return "";
    }
}
