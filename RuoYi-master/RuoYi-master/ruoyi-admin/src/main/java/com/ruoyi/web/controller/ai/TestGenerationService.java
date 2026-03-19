package com.ruoyi.web.controller.ai;

import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * AI测试用例生成服务（第五步）
 * 
 * 根据生成的代码和需求，自动生成JUnit5单元测试和MockMvc集成测试
 * 
 * @author ruoyi
 */
@Service
public class TestGenerationService
{
    private static final Logger log = LoggerFactory.getLogger(TestGenerationService.class);

    @Autowired
    private AiChatService aiChatService;

    /** 测试生成专用System Prompt */
    private static final String TEST_GEN_SYSTEM_PROMPT = """
            你是一个RuoYi v4.8.2框架的测试专家。请生成完整、可运行的测试代码。

            【测试框架】
            - JUnit 5 (Jupiter)
            - Mockito 5.x
            - Spring Boot Test
            - MockMvc（Controller层测试）

            【测试规范】
            - Service测试：使用@ExtendWith(MockitoExtension.class)，@Mock Mapper，@InjectMocks ServiceImpl
            - Controller测试：使用@WebMvcTest或MockMvc，模拟Shiro认证
            - 每个CRUD方法至少3个测试用例（正常、边界、异常）
            - 使用assertThat或assertEquals断言
            - 测试方法命名：test_方法名_场景_期望结果

            【输出要求】
            只输出代码，不输出解释。每个文件用以下格式分隔：
            === FILE: 文件路径 ===
            代码内容
            === END FILE ===
            """;

    /**
     * 根据代码生成Service层单元测试
     *
     * @param serviceCode Service接口和实现类的代码
     * @param requirementJson 结构化需求JSON
     * @return 测试代码Map
     */
    public Map<String, String> generateServiceTests(String serviceCode, String requirementJson)
    {
        String prompt = """
                根据以下Service代码和需求JSON，生成Service层JUnit5单元测试。

                要求：
                1. 使用@ExtendWith(MockitoExtension.class)
                2. @Mock注入Mapper
                3. @InjectMocks注入ServiceImpl
                4. 测试所有CRUD方法：
                   - selectList：正常查询、空结果
                   - selectById：正常查询、不存在
                   - insert：正常新增、必填字段校验
                   - update：正常修改
                   - deleteByIds：正常删除、空参数
                5. 如有业务校验（唯一性、级联删除检查），测试校验逻辑
                6. 使用when().thenReturn()模拟Mapper返回
                7. 使用verify()验证Mapper调用

                输出格式：
                === FILE: test/service/ClassNameServiceTest.java ===
                代码
                === END FILE ===

                【Service代码】
                """ + serviceCode + """

                【需求JSON】
                """ + requirementJson;

        String result = aiChatService.chat(prompt, TEST_GEN_SYSTEM_PROMPT);
        Map<String, String> files = new LinkedHashMap<>();
        parseFiles(result, files);
        log.info("Service测试生成完成，共{}个文件", files.size());
        return files;
    }

    /**
     * 根据代码生成Controller层集成测试
     *
     * @param controllerCode Controller代码
     * @param requirementJson 结构化需求JSON
     * @return 测试代码Map
     */
    public Map<String, String> generateControllerTests(String controllerCode, String requirementJson)
    {
        String prompt = """
                根据以下Controller代码和需求JSON，生成Controller层MockMvc集成测试。

                要求：
                1. 使用@SpringBootTest + @AutoConfigureMockMvc
                2. 模拟Shiro认证（绕过权限检查）
                3. 测试所有端点：
                   - GET /module/business → 页面跳转200
                   - POST /module/business/list → JSON列表返回
                   - POST /module/business/add → 新增成功
                   - POST /module/business/edit → 修改成功
                   - POST /module/business/remove → 删除成功
                   - POST /module/business/export → 导出成功
                4. 验证返回的JSON结构（code/msg）
                5. 测试参数校验（缺少必填字段）
                6. 使用MockMvc的perform/andExpect链式调用

                输出格式：
                === FILE: test/controller/ClassNameControllerTest.java ===
                代码
                === END FILE ===

                【Controller代码】
                """ + controllerCode + """

                【需求JSON】
                """ + requirementJson;

        String result = aiChatService.chat(prompt, TEST_GEN_SYSTEM_PROMPT);
        Map<String, String> files = new LinkedHashMap<>();
        parseFiles(result, files);
        log.info("Controller测试生成完成，共{}个文件", files.size());
        return files;
    }

    /**
     * 一键生成全部测试
     *
     * @param generatedCode 所有生成的代码内容
     * @param requirementJson 结构化需求JSON
     * @return 所有测试文件Map
     */
    public Map<String, String> generateAllTests(String generatedCode, String requirementJson)
    {
        Map<String, String> allTests = new LinkedHashMap<>();

        try
        {
            Map<String, String> serviceTests = generateServiceTests(generatedCode, requirementJson);
            allTests.putAll(serviceTests);

            Map<String, String> controllerTests = generateControllerTests(generatedCode, requirementJson);
            allTests.putAll(controllerTests);

            log.info("全部测试生成完成，共{}个文件", allTests.size());
        }
        catch (Exception e)
        {
            log.error("测试生成失败", e);
            allTests.put("ERROR", "测试生成失败：" + e.getMessage());
        }

        return allTests;
    }

    /**
     * 解析AI返回的文件内容
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
            fileContent = fileContent.replaceAll("^```\\w*\\n?", "").replaceAll("\\n?```$", "").trim();

            if (!filePath.isEmpty() && !fileContent.isEmpty())
            {
                files.put(filePath, fileContent);
            }
        }
    }
}
