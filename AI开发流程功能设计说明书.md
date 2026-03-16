# AI驱动软件开发流程 — 功能设计说明书

> 对应文件：AI_CODE_GENERATION_WORKFLOW.md  
> 角色：功能设计师  
> 日期：2026-03-15  
> 说明：本文档对流程中每一个步骤进行功能设计，明确"做什么、怎么做、谁来做、输入输出、与其他步骤的关系"。

---

## 总体流程概览

```
第0步 → 第一步 → 第二步 → 第三步 → 第四步 → 第五步 → 第六步 → 第七步 → 第八步 → 第九步
 AI       人+AI     AI      人工      AI       AI      人+自动    人工     人+AI+自动  人+AI
学习      需求      设计     评审      编码     测试     评审      部署      执行测试   迭代
```

**核心设计原则：**
- 每一步的输出 = 下一步的输入（链式传递）
- AI负责生成，人工负责审查（AI做80%，人做20%）
- 每一步都有质量门禁，不通过不能进入下一步

---

## 第零步：AI框架规范学习

### 功能定位

**一句话：** 让AI"读懂"RuoYi框架的编码规范，形成标准化的Prompt上下文模板。

### 为什么需要这一步

如果跳过这一步直接让AI写代码，AI会按照通用Spring Boot的方式写，而不是RuoYi的方式。比如AI会用`@RestController`而不是`@Controller`，会用Lombok而不是手写getter/setter。这一步就是给AI"立规矩"。

### 功能设计

| 子任务 | 做什么 | 怎么做 | 谁做 |
|--------|-------|--------|------|
| 0.1 注入规范Prompt | 把RuoYi编码规范整理成AI能理解的Prompt | 参考`AI_CODE_GENERATION_FRAMEWORK_DESIGN.md`第五章，把技术栈约束、18项编码红线、代码模板整理成一段系统级Prompt | 开发者 |
| 0.2 提供基类源码 | 让AI看到BaseEntity/TreeEntity/BaseController的真实代码 | 把源码文件内容直接喂给AI，或放入RAG知识库 | 开发者 |
| 0.3 提供现有清单 | 让AI知道已有哪些表、哪些菜单、哪些字典 | 把数据库表清单、菜单树、字典列表整理给AI | 开发者 |
| 0.4 验证AI理解 | 确认AI真的理解了规范 | 让AI生成一个简单的单表CRUD（如"公告管理"），检查是否符合18项规范 | 开发者检查 |

### 输入 → 输出

```
输入：RuoYi源码、AI_CODE_GENERATION_FRAMEWORK_DESIGN.md、数据库表清单
输出：经验证的AI Prompt上下文模板（可反复使用）
```

### 与其他步骤的关系

- **被依赖于：** 第一步到第九步（所有后续步骤的AI操作都依赖这个Prompt上下文）
- **特点：** 只需要做一次，除非RuoYi升级或编码规范变更

### 质量门禁

- [ ] AI生成的示例代码通过18项规范检查
- [ ] AI能正确回答"RuoYi中Controller应该用什么注解？"等基础问题

---

## 第一步：需求文档完整性检查（人工+AI）

### 功能定位

**一句话：** 把模糊的业务需求变成结构化的、可执行的"结构化需求JSON"。

### 为什么需要这一步

开发者拿到的需求通常是一段文字描述或一个Word文档，比如"做一个项目管理功能"。这太模糊了，无法直接生成代码。这一步就是把"模糊需求"翻译成"精确规格"。

### 功能设计

| 子任务 | 做什么 | 怎么做 | 谁做 |
|--------|-------|--------|------|
| 1.1 上传需求文档 | 提供原始需求 | 上传Word/Markdown/PDF文件 | 人工 |
| 1.2 上传UI风格说明 | 提供页面设计参考 | 说明页面风格（RuoYi是Bootstrap 3.3.7 + Hplus主题） | 人工 |
| 1.3 AI文档解析 | 从需求文档中提取关键信息 | AI读取文档，提取功能点清单、角色列表、业务流程、数据实体和字段、非功能需求 | AI |
| 1.4 RuoYi适配分析 | 判断哪些功能可以复用框架已有能力 | AI逐条检查：是否和内置功能重叠？角色是否可复用？字段是否用字典？是否需要@DataScope？是否TreeEntity？是否主子表？是否需要定时任务/文件上传/Excel？ | AI |
| 1.5 完整性评估 | 检查需求是否有遗漏 | AI检查：角色权限是否定义完整？业务流程是否有分支？数据字段是否有类型和约束？UI交互是否有说明？异常处理是否有考虑？ | AI |
| 1.6 生成完整性报告 | 输出需求评估结论 | AI生成报告，标注"完整/部分缺失/严重缺失"，列出具体缺失项 | AI |
| 1.7 生成结构化需求JSON | 把需求转成机器可读的格式 | AI按照固定Schema输出JSON，包含module/entities/fields/dictionaries/dataPermissions/subTables | AI |
| 1.8 人工审查补充 | 纠正AI的错误，补充遗漏 | 开发者审查JSON，修改不合理的地方，补充AI没有想到的字段或逻辑 | 人工 |

### 输入 → 输出

```
输入：需求文档（Word/MD/PDF）、UI风格说明
输出：
  ① 需求完整性报告（哪些需求完整、哪些缺失、建议补充什么）
  ② RuoYi适配分析（哪些复用、哪些新建、哪些用字典等）
  ③ 结构化需求JSON（后续所有步骤的源头数据）
```

### 结构化需求JSON示例

```json
{
  "module": "project",
  "moduleName": "项目管理",
  "packageName": "com.ruoyi.project",
  "entities": [
    {
      "tableName": "proj_project",
      "className": "ProjProject",
      "tableComment": "项目管理表",
      "tplCategory": "crud",
      "fields": [
        {"columnName": "project_id", "javaField": "projectId", "javaType": "Long", "isPk": true, "isRequired": true},
        {"columnName": "project_name", "javaField": "projectName", "javaType": "String", "isRequired": true, "queryType": "LIKE"},
        {"columnName": "project_status", "javaField": "projectStatus", "javaType": "String", "dictType": "proj_project_status", "queryType": "EQ"}
      ]
    }
  ],
  "dictionaries": [
    {"dictType": "proj_project_status", "dictName": "项目状态", "items": [
      {"label": "筹备中", "value": "0"},
      {"label": "进行中", "value": "1"},
      {"label": "已完成", "value": "2"}
    ]}
  ],
  "dataPermissions": [
    {"entity": "ProjProject", "scopeField": "dept_id", "scopeType": "deptAndChild"}
  ]
}
```

### 与其他步骤的关系

- **依赖于：** 第零步（AI需要已学习RuoYi规范才能做适配分析）
- **被依赖于：** 第二步（结构化需求JSON是第二步的输入）
- **回退点：** 如果第三步评审发现设计不合理，可能需要回退到这里修改需求

### 质量门禁

- [ ] 结构化需求JSON格式合法，所有必填字段都有值
- [ ] 每个实体至少有：主键ID、业务名称、状态字段、BaseEntity字段
- [ ] 所有枚举字段都定义了对应的字典
- [ ] 人工审查确认JSON与原始需求一致

---

## 第二步：AI生成数据库与模块设计

### 功能定位

**一句话：** 根据结构化需求JSON，自动生成数据库DDL、菜单SQL、字典SQL和设计文档。

### 为什么需要这一步

有了结构化需求JSON后，数据库设计和SQL脚本的生成是高度模板化的工作。让AI做可以保证格式统一、不遗漏BaseEntity字段、菜单层级正确。

### 功能设计

| 子任务 | 做什么 | 怎么做 | 谁做 |
|--------|-------|--------|------|
| 2.1 生成DDL | 建表SQL | 读取JSON中的entities和fields，生成CREATE TABLE语句。必须包含create_by/create_time/update_by/update_time/remark、主键BIGINT AUTO_INCREMENT、snake_case命名+业务前缀 | AI |
| 2.2 生成ER图 | 可视化表关系 | 用Mermaid格式画ER图，标注主外键关系，标注与sys_user/sys_dept等系统表的关联 | AI |
| 2.3 生成菜单SQL | 权限菜单 | 按M→C→F三级结构生成INSERT INTO sys_menu语句。每个实体生成1个C菜单+5个F按钮（查询/新增/修改/删除/导出） | AI |
| 2.4 生成字典SQL | 字典数据 | 读取JSON中的dictionaries，生成INSERT INTO sys_dict_type和sys_dict_data语句 | AI |
| 2.5 生成设计文档 | 技术方案 | 生成类图（Domain/Mapper/Service/Controller层级）、关键时序图（如新增流程）、数据权限方案 | AI |
| 2.6 质量检查 | 自动验证 | 检查命名是否snake_case、是否有BaseEntity字段、菜单层级是否完整、权限标识格式是否为module:entity:action | AI |

### 输入 → 输出

```
输入：结构化需求JSON（来自第一步）
输出：
  ① create_table.sql（DDL）
  ② er_diagram.md（Mermaid ER图）
  ③ menu.sql（菜单权限SQL）
  ④ dict.sql（字典SQL）
  ⑤ 模块设计文档（类图+时序图+数据权限方案）
  ⑥ 质检报告（检查项 + 通过/不通过）
```

### 与其他步骤的关系

- **依赖于：** 第一步（需要结构化需求JSON）
- **被依赖于：** 第三步（设计文档需要人工评审）、第四步（DDL和设计是代码生成的依据）、第七步（SQL需要在部署时执行）
- **关键约束：** 这一步生成的SQL是后续所有步骤的基础，如果SQL有问题，后面全部出错

### 质量门禁

- [ ] DDL所有表都包含create_by/create_time/update_by/update_time/remark
- [ ] DDL所有主键都是BIGINT AUTO_INCREMENT
- [ ] DDL命名全部是snake_case + 业务前缀
- [ ] 菜单SQL层级完整：每个实体有M目录→C菜单→5个F按钮
- [ ] 权限标识格式统一：module:entity:action
- [ ] 字典SQL每个类型有对应的数据条目

---

## 第三步：设计文档评审（人工）

### 功能定位

**一句话：** 人工审查AI生成的设计，确保数据库设计合理、权限正确、可执行。

### 为什么需要这一步

AI生成的设计可能有以下问题：表结构不合理、索引缺失、菜单层级错误、权限标识命名不规范、数据权限方案有漏洞。这些问题只有有经验的人才能发现。

### 功能设计

| 子任务 | 做什么 | 审查要点 | 谁做 |
|--------|-------|---------|------|
| 3.1 架构审查 | 模块结构 | 新模块放ruoyi-system扩展还是独立module？数据权限@DataScope用哪个字段？实体间关联是否合理？ | 架构师/高级开发 |
| 3.2 数据库审查 | 表设计 | BaseEntity字段齐全？索引是否合理（查询条件字段要加索引）？字段类型是否恰当？性能是否有隐患？ | 开发者 |
| 3.3 菜单权限审查 | 权限体系 | M→C→F层级是否正确？权限标识命名是否统一？数据权限范围是否合理？ | 开发者 |
| 3.4 字典数据审查 | 字典 | 编码是否从0开始？是否有可复用的已有字典（如sys_normal_disable）？标签是否清晰？ | 开发者 |
| 3.5 修改反馈 | 修正问题 | 标注具体问题和修改建议，要求AI重新生成（回到第二步） | 人工 |
| 3.6 版本标记 | 确认通过 | 评审通过后标记版本号v1.0 | 人工 |

### 输入 → 输出

```
输入：第二步的所有输出（DDL、菜单SQL、字典SQL、设计文档、质检报告）
输出：
  ✓ 评审通过 → 进入第四步
  ✗ 评审不通过 → 修改意见，回到第二步（或第一步）重新生成
```

### 与其他步骤的关系

- **依赖于：** 第二步（审查第二步的输出）
- **被依赖于：** 第四步（只有评审通过才能开始生成代码）
- **可能回退到：** 第二步（修改设计）或第一步（修改需求）

### 质量门禁

- [ ] 所有审查项都有人签字确认
- [ ] 发现的问题都已修复
- [ ] 版本号已标记

---

## 第四步：AI生成代码（核心步骤）

### 功能定位

**一句话：** 根据评审通过的设计，AI自动生成全套Java代码和HTML页面，且100%符合RuoYi规范。

### 为什么需要这一步

这是整个流程中**工作量最大、最容易出错**的步骤。手动编写一个完整的CRUD模块（Domain+Mapper+XML+Service+Controller+HTML）需要数小时，AI可以在几分钟内完成，前提是有了前面步骤的精确规格。

### 功能设计

| 子任务 | 生成物 | 关键规范 | 数量 |
|--------|-------|---------|------|
| 4.1 Domain实体类 | ProjProject.java等 | extends BaseEntity，手写getter/setter，@Excel注解，@Xss防护，@NotBlank验证，toString用ToStringBuilder | 每个表1个 |
| 4.2 Mapper接口 | ProjProjectMapper.java等 | 标准方法：selectList/selectById/insertXxx/updateXxx/deleteXxxByIds/deleteXxxById | 每个表1个 |
| 4.3 Mapper XML | ProjProjectMapper.xml等 | resultMap映射，动态SQL(`<if test>`)，日期范围查询，数据权限占位`${params.dataScope}`，`<trim>`，`<foreach>` | 每个表1个 |
| 4.4 Service接口 | IProjProjectService.java等 | I开头命名，与Mapper方法一一对应 | 每个表1个 |
| 4.5 ServiceImpl | ProjProjectServiceImpl.java等 | @Service，insert设createTime，update设updateTime，Convert.toStrArray处理批量删除 | 每个表1个 |
| 4.6 Controller | ProjProjectController.java等 | extends BaseController，@Controller，@RequestMapping，@RequiresPermissions，@Log，startPage()，@ResponseBody，返回AjaxResult/TableDataInfo | 每个表1个 |
| 4.7 HTML页面 | proj.html/add.html/edit.html | Thymeleaf模板，include::header/footer，bootstrap-table，shiro:hasPermission控制按钮，字典th:with翻译 | 每个表3个 |
| 4.8 SQL脚本包 | create_table.sql + menu.sql + dict.sql | 第二步生成的SQL合并打包 | 3个文件 |

### 18项框架规范质检

这是**强制检查**，每一项不通过都不能交付：

| # | 检查项 | 说明 | 自动化 |
|---|--------|------|:------:|
| 1 | Domain继承BaseEntity/TreeEntity | 不能直接implements Serializable | ✅ |
| 2 | 无Lombok | 不能有@Data/@Getter/@Setter | ✅ |
| 3 | @Excel注解 | 列表字段必须有@Excel用于导出 | ✅ |
| 4 | toString用ToStringBuilder | 不能用IDE自动生成的toString | ✅ |
| 5 | Controller继承BaseController | 不能直接new Controller | ✅ |
| 6 | @Controller | 不能用@RestController | ✅ |
| 7 | @RequiresPermissions | 每个方法都必须有权限注解 | ✅ |
| 8 | @Log注解 | 增删改操作必须记录日志 | ✅ |
| 9 | startPage()分页 | 列表查询必须调用startPage() | ✅ |
| 10 | Service接口I开头 | IProjProjectService不是ProjProjectService | ✅ |
| 11 | @Service注解 | ServiceImpl必须有@Service | ✅ |
| 12 | XML有resultMap | 不能用resultType | ✅ |
| 13 | XML用动态SQL | 查询条件用`<if test>` | ✅ |
| 14 | HTML引用include | header/footer/sidebar通过include引入 | ✅ |
| 15 | bootstrap-table | 列表页必须用bootstrap-table | ✅ |
| 16 | shiro:hasPermission | 按钮必须用shiro标签控制显示 | ✅ |
| 17 | 菜单SQL完整 | 目录→菜单→5个按钮 | ✅ |
| 18 | DDL含BaseEntity字段 | 5个字段一个不能少 | ✅ |

### 代码集成方式

**方式一：ruoyi-system扩展（推荐，适合大多数业务）**
```
Domain/Mapper/Service → ruoyi-system/java/com/ruoyi/{module}/
Mapper XML → ruoyi-system/resources/mapper/{module}/
Controller → ruoyi-admin/controller/{module}/
HTML → ruoyi-admin/templates/{module}/{business}/
```

**方式二：独立Maven模块（复杂业务或需要独立部署时）**
```
1. 新建 ruoyi-{module} 模块
2. 父pom.xml添加 <module>ruoyi-{module}</module>
3. ruoyi-admin的pom.xml添加 <dependency>ruoyi-{module}</dependency>
4. Controller和HTML仍然放在ruoyi-admin中
```

### 输入 → 输出

```
输入：评审通过的设计（DDL、菜单SQL、字典SQL、设计文档）
输出：
  ① Java源码（Domain/Mapper/Service/Controller）
  ② Mapper XML
  ③ HTML模板（列表/新增/编辑）
  ④ SQL脚本包
  ⑤ 18项质检报告（全部通过才能进入下一步）
```

### 与其他步骤的关系

- **依赖于：** 第三步（必须评审通过）
- **被依赖于：** 第五步（测试基于生成的代码）、第六步（评审生成的代码）、第七步（部署生成的代码）
- **关键风险：** 这是最容易出错的步骤，所以后面有第五步测试和第六步评审两道关卡

### 质量门禁

- [ ] 18项框架规范全部通过
- [ ] 代码能编译通过（mvn compile无报错）
- [ ] 所有文件放在正确的目录

---

## 第五步：AI生成测试用例

### 功能定位

**一句话：** AI根据需求+代码，自动生成测试用例文档和测试代码。

### 为什么需要这一步

代码写完不等于做完。没有测试的代码不可信。让AI生成测试用例可以覆盖大部分常规场景，减少人工编写测试的工作量。

### 功能设计

| 子任务 | 做什么 | 覆盖范围 | 谁做 |
|--------|-------|---------|------|
| 5.1 功能测试用例文档 | 文本格式的测试用例 | 正常流程（CRUD每个操作）、边界值（最大长度、空值）、异常（不存在的ID、重复数据）、权限（无权限访问）、数据权限（跨部门访问）、Excel导入导出 | AI |
| 5.2 Service单元测试 | JUnit5代码 | 用Mockito模拟Mapper，测试Service层的业务逻辑，覆盖率>=80% | AI |
| 5.3 Controller集成测试 | MockMvc代码 | 测试HTTP请求→响应，验证AjaxResult格式、TableDataInfo分页、权限拦截、@Validated验证 | AI |
| 5.4 测试数据SQL | 用于测试的数据 | 覆盖各种状态的数据、边界值数据、用于清理的DELETE语句 | AI |

### 输入 → 输出

```
输入：需求文档、设计文档、生成的Java代码
输出：
  ① 功能测试用例文档（Excel或Markdown表格）
  ② JUnit5单元测试代码（*ServiceTest.java）
  ③ MockMvc集成测试代码（*ControllerTest.java）
  ④ 测试数据SQL（test_data.sql + cleanup.sql）
```

### 与其他步骤的关系

- **依赖于：** 第四步（基于生成的代码编写测试）
- **被依赖于：** 第六步（评审测试质量）、第八步（执行这些测试）

### 质量门禁

- [ ] 功能测试用例覆盖所有CRUD操作
- [ ] 单元测试代码能编译通过
- [ ] 每个Service方法至少有一个测试

---

## 第六步：代码和测试评审（人工+自动化）

### 功能定位

**一句话：** 人工+自动化工具联合审查代码和测试质量，确保可以部署。

### 为什么需要这一步

AI生成的代码可能有逻辑错误（比如删除项目时没检查子任务），可能没有用RuoYi内置工具类（比如自己写了日期格式化而不用DateUtils），可能有安全隐患。这些问题需要人来发现。

### 功能设计

| 子任务 | 做什么 | 审查要点 | 谁做 |
|--------|-------|---------|------|
| 6.1 自动规范检查 | 跑18项规范扫描 | 自动化脚本检查代码是否符合RuoYi规范 | 自动化 |
| 6.2 开发人员审查 | 代码逻辑 | 业务逻辑是否正确？是否用了RuoYi内置工具类（DateUtils/StringUtils/Convert等）？SQL是否有性能问题？是否有安全漏洞（SQL注入/XSS）？ | 开发者 |
| 6.3 测试人员审查 | 测试覆盖 | 测试用例是否覆盖所有业务场景？权限测试是否完整？数据权限场景是否覆盖？ | 测试者 |
| 6.4 修复问题 | 修改代码 | 根据审查意见修改代码和测试 | AI+人工 |
| 6.5 版本标记 | Git管理 | 代码合并到主分支，标记版本号 | 开发者 |
| 6.6 审查报告 | 记录结论 | 生成审查报告，记录发现的问题和修复情况 | 开发者 |

### 输入 → 输出

```
输入：第四步的代码 + 第五步的测试
输出：
  ✓ 审查通过 → 进入第七步部署
  ✗ 审查不通过 → 修改后重新审查
```

### 与其他步骤的关系

- **依赖于：** 第四步（审查代码）、第五步（审查测试）
- **被依赖于：** 第七步（审查通过才能部署）
- **可能回退到：** 第四步（修改代码）或第五步（补充测试）

---

## 第七步：部署系统

### 功能定位

**一句话：** 把代码部署到运行环境，让系统跑起来。

### 为什么需要这一步

代码在IDE里能编译通过不等于能正常运行。需要部署到实际环境验证数据库连接、SQL脚本执行、菜单权限、页面渲染等。

### 功能设计

| 子任务 | 做什么 | 具体操作 | 谁做 |
|--------|-------|---------|------|
| 7.1 环境准备 | 确认软件版本 | JDK 17+、MySQL 5.7+/8.0+、Maven 3.6+ | 运维/开发 |
| 7.2 初始化数据库 | 执行SQL | ①创建数据库 ②执行ry_20250416.sql ③执行quartz.sql ④执行新模块的SQL（create_table→dict→menu） | 开发者 |
| 7.3 修改配置 | 适配环境 | 修改application-druid.yml（数据库地址/用户名/密码）、application.yml（端口/上传路径） | 开发者 |
| 7.4 编译启动 | 运行系统 | 开发环境：IDE运行RuoYiApplication.java；生产环境：mvn clean package → java -jar ruoyi-admin.jar | 开发者 |
| 7.5 验证 | 检查系统 | 访问http://localhost:80，用admin/admin123登录，验证新模块菜单出现，CRUD功能正常 | 开发者 |

### 输入 → 输出

```
输入：审查通过的代码 + SQL脚本 + 配置文件
输出：运行中的系统（http://localhost:80）
```

### 与其他步骤的关系

- **依赖于：** 第六步（审查通过）
- **被依赖于：** 第八步（在运行的系统上执行测试）

### 关键注意事项

- RuoYi是**前后端不分离**的，部署只需要一个JAR包，不需要单独启动前端
- SQL脚本必须按顺序执行：建表 → 字典 → 菜单
- 新增菜单后需要在角色管理中分配权限，否则看不到菜单

---

## 第八步：执行测试（AI+人工）

### 功能定位

**一句话：** 在运行的系统上执行所有测试，生成测试报告和Bug清单。

### 功能设计

| 子任务 | 做什么 | 怎么做 | 谁做 |
|--------|-------|--------|------|
| 8.1 自动单元测试 | 跑JUnit | `mvn test`，JaCoCo收集覆盖率，目标>=80% | 自动化 |
| 8.2 集成测试 | 跑MockMvc | 自动执行Controller集成测试 | 自动化 |
| 8.3 人工UI测试 | 手动点击验证 | 列表展示、搜索、分页、表单提交、删除确认、Excel导出、权限控制（不同角色看到不同按钮）、浏览器兼容 | 人工 |
| 8.4 业务流程测试 | 端到端验证 | 模拟完整业务流程（如：创建项目→添加任务→完成任务→关闭项目），验证数据一致性、并发安全 | 人工 |
| 8.5 AI分析结果 | 生成Bug清单 | AI分析测试报告和失败用例，分类整理Bug（严重/一般/轻微） | AI |
| 8.6 测试报告 | 总结文档 | 生成包含通过率、覆盖率、Bug清单、未覆盖场景的测试报告 | AI+人工 |

### 输入 → 输出

```
输入：运行中的系统 + 测试用例 + 测试代码
输出：
  ① 测试报告（通过率、覆盖率统计）
  ② Bug清单（含严重级别和复现步骤）
  ✓ 全部通过 → 交付
  ✗ 有Bug → 进入第九步迭代
```

### 与其他步骤的关系

- **依赖于：** 第七步（系统必须在运行中）
- **被依赖于：** 第九步（Bug清单是迭代的输入）
- **终结点：** 如果测试全部通过，流程结束，项目交付

---

## 第九步：修改和迭代

### 功能定位

**一句话：** 根据Bug或变更需求，AI分析影响范围，增量修改，重新走一遍流程。

### 为什么需要这一步

软件开发不可能一次做对。测试发现Bug、用户提出新需求、设计考虑不周都会导致需要迭代。这一步的核心是**增量更新而非全量重做**。

### 功能设计

| 子任务 | 做什么 | 怎么做 | 谁做 |
|--------|-------|--------|------|
| 9.1 分析Bug原因 | 定位问题根源 | 判断是需求问题（需求不明确）、设计问题（表结构不合理）还是代码问题（逻辑错误） | AI+人工 |
| 9.2 是否需要改需求/设计 | 判断影响层级 | 如果是代码Bug→只改代码；如果是设计问题→改设计+代码；如果是需求问题→改需求+设计+代码 | 人工 |
| 9.3 修改需求文档 | 更新需求 | 在原需求JSON中标注修改点，版本号+1 | 人工 |
| 9.4 AI影响分析 | 确定修改范围 | AI分析：改了哪些表？影响哪些Domain/Mapper/Service/Controller？影响哪些HTML？需要新增菜单？需要修改测试？ | AI |
| 9.5 AI增量生成 | 只重新生成受影响的部分 | 数据库用ALTER TABLE而不是DROP+CREATE，代码只修改变更的文件 | AI |
| 9.6 重新走流程 | 回到第三步 | 从设计评审（第三步）重新开始走到测试（第八步） | 按流程 |
| 9.7 版本管理 | 记录变更 | Git commit记录变更原因和内容，tag标记新版本 | 开发者 |

### 关键设计原则

- **增量更新：** 只改受影响的部分，不全量重做
- **变更追踪：** 每次变更记录原因、范围、时间
- **版本管理：** Git tag标记版本，支持回滚
- **影响分析：** AI自动分析"改了A会影响B和C"
- **回归测试：** 变更后必须跑完整回归测试

### 输入 → 输出

```
输入：Bug清单 或 变更需求
输出：增量修改的代码、SQL、测试 → 重新回到第三步评审
```

### 与其他步骤的关系

- **依赖于：** 第八步（Bug清单触发迭代）
- **回退到：** 第三步（重新评审修改后的设计）
- **循环：** 第九步→第三步→第四步→...→第八步→（如果还有Bug）→第九步

---

## 步骤间依赖关系总图

```
第0步（一次性）─→ Prompt上下文
                    │
                    ▼
              ┌─ 第一步 ─┐
              │  需求分析  │ → 结构化需求JSON
              └────┬──────┘
                   ▼
              ┌─ 第二步 ─┐
              │  AI设计   │ → DDL + 菜单SQL + 字典SQL + 设计文档
              └────┬──────┘
                   ▼
              ┌─ 第三步 ─┐     ✗ 不通过
              │  人工评审  │ ──────→ 回到第一步或第二步
              └────┬──────┘
                   │ ✓ 通过
                   ▼
              ┌─ 第四步 ─┐
              │  AI生成代码│ → Java + HTML + SQL + 18项质检
              └────┬──────┘
                   ▼
              ┌─ 第五步 ─┐
              │  AI生成测试│ → JUnit5 + MockMvc + 测试用例
              └────┬──────┘
                   ▼
              ┌─ 第六步 ─┐     ✗ 不通过
              │  代码评审  │ ──────→ 回到第四步或第五步
              └────┬──────┘
                   │ ✓ 通过
                   ▼
              ┌─ 第七步 ─┐
              │   部署    │ → 运行中的系统
              └────┬──────┘
                   ▼
              ┌─ 第八步 ─┐     ✓ 全部通过 → 交付完成
              │  执行测试  │
              └────┬──────┘
                   │ ✗ 有Bug
                   ▼
              ┌─ 第九步 ─┐
              │  迭代修改  │ ──────→ 回到第三步
              └──────────┘
```

---

## 各步骤工作量预估

| 步骤 | AI工作量 | 人工工作量 | 总耗时预估 |
|------|---------|-----------|-----------|
| 第0步 | 20% | 80% | 2-4小时（一次性） |
| 第一步 | 60% | 40% | 1-2小时 |
| 第二步 | 90% | 10% | 30分钟 |
| 第三步 | 0% | 100% | 1-2小时 |
| 第四步 | 95% | 5% | 30分钟（AI）+ 30分钟（检查） |
| 第五步 | 90% | 10% | 30分钟 |
| 第六步 | 30% | 70% | 1-2小时 |
| 第七步 | 0% | 100% | 30分钟 |
| 第八步 | 30% | 70% | 2-4小时 |
| 第九步 | 50% | 50% | 视Bug数量 |

**一个标准CRUD模块从需求到交付：预计1-2天**（传统方式需3-5天）
