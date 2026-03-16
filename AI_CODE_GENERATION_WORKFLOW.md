# AI驱动软件开发流程设计方案（基于RuoYi v4.8.2）

> 版本：v2.0（重构版）  
> 日期：2025-03-15  
> 目标框架：RuoYi v4.8.2（Spring Boot 4.0.3 + Thymeleaf + Bootstrap，前后端不分离）  
> JDK版本：17+  
> 数据库：MySQL

---

## 技术栈约束（贯穿全流程）

| 层次 | 技术 | 版本 | 关键约束 |
|------|------|------|---------|
| 基础框架 | Spring Boot | 4.0.3 | JDK 17+，Jakarta EE |
| 安全框架 | Apache Shiro | 2.1.0 | `@RequiresPermissions` 控制权限 |
| 持久层 | MyBatis | 4.0.1 | Mapper接口 + XML |
| 连接池 | Druid | 1.2.28 | 监控面板 `/druid` |
| 分页 | PageHelper | 2.1.1 | `startPage()` 开启分页 |
| 模板引擎 | Thymeleaf | — | **前后端不分离**，服务端渲染HTML |
| 前端UI | Bootstrap 3.3.7 + jQuery | — | bootstrap-table、layer.js弹窗 |
| JSON | Fastjson | 1.2.83 | — |
| API文档 | springdoc-openapi | 3.0.2 | 替代旧版Swagger/Springfox |
| 代码生成 | Velocity | 2.3 | 框架内置代码生成器 |
| 缓存 | EhCache | — | Shiro会话缓存 |
| 定时任务 | Quartz | — | 内置任务调度 |
| Excel | Apache POI | 4.1.2 | `@Excel`注解 + `ExcelUtil` |

**核心编码红线：**
- ❌ **不使用Lombok**，手写getter/setter
- ❌ **不使用`@RestController`**，使用`@Controller` + `@ResponseBody`
- ❌ **前端不是Vue/React**，是Thymeleaf HTML模板
- ✅ Domain必须继承`BaseEntity`或`TreeEntity`
- ✅ Controller必须继承`BaseController`
- ✅ 每个方法必须有`@RequiresPermissions`权限注解
- ✅ 增删改操作必须有`@Log`日志注解

---

## RuoYi内置功能（不需要重复开发）

| # | 功能 | 新模块复用方式 |
|---|------|-------------|
| 1 | 用户管理 | 通过`create_by`关联用户 |
| 2 | 部门管理 | `@DataScope`复用数据权限 |
| 3 | 角色管理 | 新模块生成菜单后自动纳入角色体系 |
| 4 | 菜单管理 | **必须为新模块生成菜单SQL** |
| 5 | 字典管理 | 枚举字段统一使用`sys_dict` |
| 6 | 参数管理 | 配置可存入`sys_config` |
| 7 | 操作日志 | Controller加`@Log`注解 |
| 8 | 代码生成 | 可作为AI生成的参照 |
| 9 | 定时任务 | 新任务实现`AbstractQuartzJob` |
| 10 | 服务/缓存/连接池监控 | 直接复用 |

---

## 模块架构（代码放置位置）

```
ruoyi（父工程）
├── ruoyi-admin        ← Web入口（启动类RuoYiApplication）
│   ├── controller/{module}/  ← 【新Controller放这里】
│   ├── templates/{module}/   ← 【新HTML模板放这里】
│   ├── application.yml       ← 主配置（端口80）
│   └── application-druid.yml ← 数据源（MySQL ry库）
├── ruoyi-system       ← 业务模块（Domain/Mapper/Service）
│   └── 【新业务的Domain/Mapper/Service放这里，或新建module】
├── ruoyi-framework    ← 框架核心（不要修改）
├── ruoyi-common       ← 通用工具（BaseEntity/BaseController/注解/工具类）
├── ruoyi-generator    ← 代码生成（Velocity模板）
├── ruoyi-quartz       ← 定时任务
└── sql/               ← 初始化脚本
```

---

## 第零步：AI框架规范学习

> 目的：让AI充分理解RuoYi编码规范

### 具体任务

- [ ] **0.1** 向AI注入RuoYi框架规范Prompt（标准模板见 `AI_CODE_GENERATION_FRAMEWORK_DESIGN.md` 第五章）
- [ ] **0.2** 提供核心基类源码：BaseEntity、TreeEntity、BaseController、示例Controller
- [ ] **0.3** 提供已有业务模块列表和数据库表清单
- [ ] **0.4** 验证AI理解（让AI生成一个简单示例做验证）

**输入：** RuoYi源码、规范文档  
**输出：** 经验证的AI Prompt上下文模板

| 工具 | 用途 | 链接 |
|------|------|------|
| LangChain | AI上下文管理 | https://github.com/langchain-ai/langchain |
| LlamaIndex | 代码库索引 | https://github.com/run-llama/llama_index |

---

## 第一步：需求文档完整性检查（人工+AI）

### 具体任务

- [ ] **1.1** 上传需求文档（Word/Markdown/PDF）
- [ ] **1.2** 上传UI风格说明（需适配Bootstrap 3.3.7 + Hplus主题）
- [ ] **1.3** AI解析文档提取：功能点、角色定义、业务流程、数据实体字段、非功能需求
- [ ] **1.4** AI进行**RuoYi适配分析**：
  - ✓ 是否与内置18个功能重叠？
  - ✓ 哪些角色可复用已有角色？
  - ✓ 哪些字段应使用字典管理？
  - ✓ 是否需要数据权限（@DataScope）？
  - ✓ 是否需要树形结构（TreeEntity）？
  - ✓ 是否有主子表关系？
  - ✓ 是否需要定时任务/文件上传/Excel导入导出？
- [ ] **1.5** AI完整性评估（角色权限、业务流程、数据字段、UI交互、异常处理等）
- [ ] **1.6** AI生成需求完整性报告
- [ ] **1.7** AI生成**结构化需求JSON**（含module/entities/fields/dictionaries/dataPermissions/subTables，详细Schema见 `AI_CODE_GENERATION_FRAMEWORK_DESIGN.md` 第五章5.2节）
- [ ] **1.8** 人工审查并补充

**输入：** 需求文档、风格说明  
**输出：** 完整性报告、RuoYi适配分析、结构化需求JSON

| 工具 | 用途 | 链接 |
|------|------|------|
| Apache Tika | 文档解析 | https://tika.apache.org/ |
| LangChain | AI文档处理 | https://github.com/langchain-ai/langchain |
| Docling | 文档智能解析 | https://github.com/DS4SD/docling |

---

## 第二步：AI生成数据库与模块设计

### 具体任务

- [ ] **2.1** 生成DDL（必含create_by/create_time/update_by/update_time/remark，主键BIGINT AUTO_INCREMENT，snake_case+业务前缀）
- [ ] **2.2** 生成ER图（Mermaid格式，标注与sys_user等系统表关联）
- [ ] **2.3** 生成**菜单权限SQL**（目录M→菜单C→5个按钮F：查询/新增/修改/删除/导出）
- [ ] **2.4** 生成**字典SQL**（sys_dict_type + sys_dict_data）
- [ ] **2.5** 生成模块设计文档（类图、时序图、数据权限方案）
- [ ] **2.6** 自动质量检查（命名规范、必须字段、菜单完整性、权限标识格式等）

**输入：** 结构化需求JSON  
**输出：** DDL、ER图、菜单SQL、字典SQL、设计文档、质检报告

| 工具 | 用途 | 链接 |
|------|------|------|
| Mermaid | ER图/类图/时序图 | https://mermaid.js.org/ |
| PlantUML | UML图 | https://plantuml.com/ |
| DBDiagram.io | 数据库设计 | https://dbdiagram.io/ |

---

## 第三步：设计文档评审（人工）

- [ ] **3.1** 架构师审查模块设计（放入ruoyi-system还是新建module？数据权限方案？）
- [ ] **3.2** DBA审查数据库设计（BaseEntity字段、索引、性能）
- [ ] **3.3** **菜单权限评审**（M→C→F层级、权限标识命名、数据权限范围）
- [ ] **3.4** 字典数据评审（编码规范、是否有可复用的已有字典如sys_normal_disable）
- [ ] **3.5** 标注问题，必要时要求AI重新生成
- [ ] **3.6** 评审通过标记版本号v1.0

**输入：** 设计文档  
**输出：** 评审通过的设计或修改意见

---

## 第四步：AI生成代码（核心步骤）

> ⚠️ 所有代码必须100%遵循RuoYi规范，详细代码模板见 `AI_CODE_GENERATION_FRAMEWORK_DESIGN.md` 第四章

### 生成清单

| # | 生成物 | 关键规范 |
|---|--------|---------|
| 4.1 | **Domain实体类** | extends BaseEntity，手写getter/setter，@Excel，@Xss，@NotBlank，ToStringBuilder |
| 4.2 | **Mapper接口** | 标准方法命名select/insert/update/delete |
| 4.3 | **Mapper XML** | resultMap、动态SQL`<if test>`、日期范围查询、数据权限占位、`<trim>`、`<foreach>` |
| 4.4 | **Service接口** | I开头命名 |
| 4.5 | **ServiceImpl** | @Service，insert设createTime，update设updateTime，Convert.toStrArray |
| 4.6 | **Controller** | extends BaseController，@Controller，@RequiresPermissions，@Log，startPage()，@ResponseBody |
| 4.7 | **Thymeleaf HTML** | include::header/footer，bootstrap-table，shiro:hasPermission，字典th:with |
| 4.8 | **SQL脚本包** | create_table.sql + menu.sql + dict.sql + init_data.sql |

### 18项框架规范质检

| # | 检查项 | 必须 |
|---|--------|------|
| 1 | Domain继承BaseEntity/TreeEntity | ✅ |
| 2 | 无Lombok，手写getter/setter | ✅ |
| 3 | @Excel注解 | ✅ |
| 4 | toString用ToStringBuilder | ✅ |
| 5 | Controller继承BaseController | ✅ |
| 6 | @Controller（非@RestController） | ✅ |
| 7 | @RequiresPermissions | ✅ |
| 8 | @Log注解 | ✅ |
| 9 | startPage()分页 | ✅ |
| 10 | Service接口I开头 | ✅ |
| 11 | @Service注解 | ✅ |
| 12 | XML有resultMap | ✅ |
| 13 | XML用动态SQL | ✅ |
| 14 | HTML引用include | ✅ |
| 15 | bootstrap-table | ✅ |
| 16 | shiro:hasPermission | ✅ |
| 17 | 菜单SQL完整（目录→菜单→5按钮） | ✅ |
| 18 | DDL含BaseEntity字段 | ✅ |

### 代码集成步骤

**方式一：ruoyi-system扩展（简单业务）**
1. Domain/Mapper/Service → `ruoyi-system/java/com/ruoyi/{module}/`
2. Mapper XML → `ruoyi-system/resources/mapper/{module}/`
3. Controller → `ruoyi-admin/controller/{module}/`
4. HTML → `ruoyi-admin/templates/{module}/{business}/`
5. 执行SQL → `mvn clean package -DskipTests`

**方式二：独立Maven模块（复杂业务）**
1. 新建`ruoyi-{module}`模块
2. 父pom.xml添加module + ruoyi-admin添加dependency
3. 按方式一放置Controller和HTML
4. 执行SQL → 重新编译

| 工具 | 用途 | 链接 |
|------|------|------|
| RuoYi Generator | 代码生成参照 | 框架内置 |
| Checkstyle | 代码格式检查 | https://checkstyle.sourceforge.io/ |
| SonarQube | 代码质量分析 | https://www.sonarqube.org/ |

---

## 第五步：AI生成测试用例

- [ ] **5.1** 功能测试用例文档（正常流程/边界值/异常/权限/数据权限/Excel导入导出）
- [ ] **5.2** Service层单元测试（JUnit5 + Mockito，覆盖率>=80%）
- [ ] **5.3** Controller集成测试（Spring MockMvc，测试AjaxResult/TableDataInfo/权限拦截/@Validated）
- [ ] **5.4** 测试数据SQL（覆盖各状态 + 边界值 + 清理SQL）

**输入：** 需求、设计、代码  
**输出：** 测试用例文档、JUnit5代码、MockMvc代码、测试数据SQL

| 工具 | 用途 | 链接 |
|------|------|------|
| JUnit5 | 单元测试 | https://junit.org/junit5/ |
| Mockito | Mock | https://site.mockito.org/ |
| MockMvc | Controller测试 | Spring内置 |
| Selenium/Playwright | Thymeleaf页面E2E测试 | https://www.selenium.dev/ |
| JMeter | 性能测试 | https://jmeter.apache.org/ |

---

## 第六步：代码和测试评审（人工+自动化）

- [ ] **6.1** 自动运行18项框架规范检查
- [ ] **6.2** 开发人员审查（逻辑、RuoYi规范、内置工具类使用、性能、安全）
- [ ] **6.3** 测试人员审查（用例覆盖、权限场景、数据权限场景）
- [ ] **6.4** 补充遗漏项，修改不合理代码
- [ ] **6.5** Git标记版本号v1.0
- [ ] **6.6** 生成审查报告

---

## 第七步：部署系统

- [ ] **7.1** 环境准备：MySQL 5.7+/8.0+、**JDK 17+**、Maven 3.6+
- [ ] **7.2** 初始化数据库：
  ```sql
  CREATE DATABASE ry DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
  ```
  执行：`sql/ry_20250416.sql` → `sql/quartz.sql` → 新模块SQL
- [ ] **7.3** 修改配置：`application-druid.yml`数据库连接、`application.yml`端口和上传路径
- [ ] **7.4** 编译启动：
  - 开发：直接运行 `RuoYiApplication.java`
  - 生产：`mvn clean package -DskipTests` → `java -jar ruoyi-admin.jar`
  - **前后端不分离，不需要单独启动前端**
- [ ] **7.5** 验证：`http://localhost:80`，账号 `admin/admin123`

---

## 第八步：执行测试（AI+人工）

- [ ] **8.1** 自动执行：`mvn test` + JaCoCo覆盖率
- [ ] **8.2** MockMvc集成测试
- [ ] **8.3** 人工UI测试（列表/搜索/分页/表单/删除/导出/权限控制/浏览器兼容）
- [ ] **8.4** 业务流程测试 + 数据一致性 + 并发
- [ ] **8.5** AI分析结果，生成Bug清单
- [ ] **8.6** 生成测试报告

| 工具 | 用途 | 链接 |
|------|------|------|
| JaCoCo | 覆盖率 | https://www.jacoco.org/ |
| Allure | 测试报告 | https://docs.qameta.io/allure/ |

---

## 第九步：修改和迭代

- [ ] **9.1** 分析Bug原因（需求/设计/代码问题）
- [ ] **9.2** 确定是否需要修改需求或设计
- [ ] **9.3** 修改需求文档（标注修改点+版本号）
- [ ] **9.4** AI分析变更影响范围（表/代码/页面/菜单/测试）
- [ ] **9.5** AI增量生成（只重新生成受影响部分，DDL用ALTER TABLE）
- [ ] **9.6** 重新执行第三步到第八步
- [ ] **9.7** Git记录变更历史

### 关键优化
- **增量更新** — 只更新受影响部分
- **变更追踪** — 记录原因、范围、时间
- **版本管理** — Git tag，支持回滚
- **影响分析** — AI自动分析变更影响
- **回归测试** — 变更后跑完整回归

| 工具 | 用途 | 链接 |
|------|------|------|
| Git | 版本控制 | https://git-scm.com/ |
| Flyway | 数据库版本管理 | https://flywaydb.org/ |
| Semantic Versioning | 版本号规范 | https://semver.org/ |

---

## 流程总图

```
第0步：AI框架规范学习 → Prompt上下文
          ↓
第一步：需求检查 + RuoYi适配分析 → 结构化需求JSON
          ↓
第二步：AI生成设计 → DDL + 菜单SQL + 字典SQL + 设计文档
          ↓
第三步：设计评审（人工）→ 评审通过
          ↓
第四步：AI生成代码 → Domain/Mapper/XML/Service/Controller/HTML + 18项质检
          ↓
第五步：AI生成测试 → JUnit5 + MockMvc + 测试用例文档
          ↓
第六步：代码评审（人工+自动化）→ 审查通过
          ↓
第七步：部署 → MySQL + mvn package + java -jar → http://localhost:80
          ↓
第八步：测试 → 测试报告 + Bug清单
          ↓
    ✓ 通过 → 交付     ✗ 失败 → 第九步增量迭代 → 回到第二步
```

---

## 快速检查清单

### 项目启动前
- [ ] JDK 17+ 已安装
- [ ] MySQL 5.7+/8.0+ 已安装
- [ ] Maven 3.6+ 已安装
- [ ] RuoYi基础SQL已执行（ry_20250416.sql + quartz.sql）
- [ ] application-druid.yml数据库连接已配置
- [ ] AI服务已配置（OpenAI API或本地LLM）
- [ ] AI已学习RuoYi框架规范Prompt

### 每个步骤完成后
- [ ] 输出物已生成
- [ ] RuoYi框架规范检查已通过
- [ ] 人工审查已完成
- [ ] 版本号已标记（Git tag）
- [ ] 文档已更新

### 项目完成后
- [ ] 所有18项框架规范检查通过
- [ ] 测试覆盖率 >= 80%
- [ ] 代码已部署运行
- [ ] 菜单权限SQL已执行，角色已分配
- [ ] 文档完整，变更历史已记录
