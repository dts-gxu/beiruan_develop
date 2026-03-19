package com.ruoyi.web.controller.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * AI代码评审服务（第六步）
 * 
 * 对AI生成的代码进行框架规范检查和质量评审
 * 
 * @author ruoyi
 */
@Service
public class CodeReviewService
{
    private static final Logger log = LoggerFactory.getLogger(CodeReviewService.class);

    @Autowired
    private AiChatService aiChatService;

    /** 代码评审专用System Prompt */
    private static final String CODE_REVIEW_SYSTEM_PROMPT = """
            你是一个严格的RuoYi v4.8.2框架代码审查专家。
            请逐条检查代码是否符合框架规范，输出评审报告。
            评审必须覆盖以下所有检查项，每项标注 ✅通过 / ⚠️警告 / ❌不通过。
            """;

    /**
     * 对生成的代码进行框架规范检查
     *
     * @param code 待评审的代码内容（所有文件拼接）
     * @return Markdown格式的评审报告
     */
    public String reviewCode(String code)
    {
        String prompt = """
                请对以下RuoYi框架代码进行全面评审，按照以下清单逐条检查：

                ## 一、Domain实体类检查
                | # | 检查项 | 结果 | 说明 |
                |---|--------|------|------|
                | 1 | 是否继承BaseEntity/TreeEntity | | |
                | 2 | 是否手写getter/setter（无Lombok） | | |
                | 3 | 是否包含@Excel注解 | | |
                | 4 | 是否包含toString方法（ToStringBuilder） | | |
                | 5 | 是否有serialVersionUID | | |
                | 6 | 字段命名是否为驼峰（camelCase） | | |
                | 7 | 日期字段是否有@JsonFormat | | |

                ## 二、Controller检查
                | # | 检查项 | 结果 | 说明 |
                |---|--------|------|------|
                | 1 | 是否继承BaseController | | |
                | 2 | 是否使用@Controller（非@RestController） | | |
                | 3 | 是否有prefix变量 | | |
                | 4 | 每个方法是否有@RequiresPermissions | | |
                | 5 | 增删改是否有@Log注解 | | |
                | 6 | 列表查询是否调用startPage() | | |
                | 7 | 新增是否设置createBy | | |
                | 8 | 修改是否设置updateBy | | |
                | 9 | 返回JSON的方法是否有@ResponseBody | | |

                ## 三、Service检查
                | # | 检查项 | 结果 | 说明 |
                |---|--------|------|------|
                | 1 | 是否有@Service注解 | | |
                | 2 | 是否@Autowired注入Mapper | | |
                | 3 | insert是否设置createTime | | |
                | 4 | update是否设置updateTime | | |
                | 5 | deleteByIds是否使用Convert.toStrArray | | |

                ## 四、Mapper XML检查
                | # | 检查项 | 结果 | 说明 |
                |---|--------|------|------|
                | 1 | 是否有resultMap | | |
                | 2 | 是否有sql片段 | | |
                | 3 | selectList是否使用动态SQL | | |
                | 4 | insert是否使用trim动态插入 | | |
                | 5 | update是否使用trim动态更新 | | |
                | 6 | 删除是否支持批量（foreach） | | |

                ## 五、SQL检查
                | # | 检查项 | 结果 | 说明 |
                |---|--------|------|------|
                | 1 | 表是否包含create_by/create_time等BaseEntity字段 | | |
                | 2 | 表名是否snake_case | | |
                | 3 | 是否生成sys_menu菜单SQL | | |
                | 4 | 按钮权限是否包含5个标准权限 | | |
                | 5 | 字典字段是否生成dict SQL | | |

                ## 六、HTML页面检查
                | # | 检查项 | 结果 | 说明 |
                |---|--------|------|------|
                | 1 | 是否引用include :: header/footer | | |
                | 2 | 是否使用bootstrap-table | | |
                | 3 | 是否使用shiro:hasPermission | | |
                | 4 | 字典字段是否用th:with获取 | | |
                | 5 | 搜索表单是否配置正确 | | |

                ## 七、总结
                - 通过项数 / 总检查项数
                - 主要问题清单
                - 修复建议

                【待评审代码】
                """ + code;

        try
        {
            String result = aiChatService.chat(prompt, CODE_REVIEW_SYSTEM_PROMPT);
            log.info("代码评审完成");
            return result;
        }
        catch (Exception e)
        {
            log.error("代码评审失败", e);
            return "代码评审失败：" + e.getMessage();
        }
    }

    /**
     * 根据评审结果自动修复代码问题
     *
     * @param originalCode 原始代码
     * @param reviewReport 评审报告
     * @return 修复后的代码
     */
    public String fixCode(String originalCode, String reviewReport)
    {
        String prompt = """
                根据以下代码评审报告中发现的问题，修复原始代码。

                要求：
                1. 只修复评审报告中标注为⚠️警告或❌不通过的问题
                2. 不改变业务逻辑
                3. 输出修复后的完整代码
                4. 在修复的地方添加注释说明修改原因

                输出格式：
                === FILE: 文件路径 ===
                修复后的完整代码
                === END FILE ===

                【评审报告】
                """ + reviewReport + """

                【原始代码】
                """ + originalCode;

        try
        {
            String result = aiChatService.chat(prompt, CODE_REVIEW_SYSTEM_PROMPT);
            log.info("代码修复完成");
            return result;
        }
        catch (Exception e)
        {
            log.error("代码修复失败", e);
            return "代码修复失败：" + e.getMessage();
        }
    }
}
