# 需求完整性报告

> 模块：项目任务管理  
> 生成日期：2026-03-16  
> AI模型：DeepSeek V3  
> 状态：✅ 可进入第二步设计阶段  

---

## 一、项目概述

| 项目 | 说明 |
|------|------|
| 功能名称 | 项目任务管理模块 |
| 功能目标 | 实现项目的创建、管理和任务跟踪，提高团队协作效率 |
| 技术框架 | RuoYi v4.8.2（Spring Boot 4.0.3 + Shiro + MyBatis + Thymeleaf） |
| 模块标识 | project |
| 表前缀 | proj_ |
| 包路径 | com.ruoyi.project |

---

## 二、功能点清单

| # | 功能名称 | 类型 | 所属模块 | 说明 |
|---|---------|------|---------|------|
| 1 | 项目管理 | CRUD | project | 项目的增删改查、导出，支持按状态和负责人筛选 |
| 2 | 任务管理 | CRUD | project | 任务的增删改查、导出，关联项目，支持分配、状态流转 |
| 3 | 项目-任务联查 | 主子表 | project | 在项目详情页查看该项目下所有任务 |
| 4 | 任务状态流转 | 业务逻辑 | project | 待处理→进行中→已完成→已关闭，支持回退 |
| 5 | 项目统计概览 | 统计 | project | 项目数量、任务完成率、超期任务数等 |

---

## 三、角色与权限

### 角色定义

| 角色 | 说明 | 数据范围 |
|------|------|---------|
| 超级管理员（admin） | 全部功能，全部数据 | 全部数据 |
| 项目经理 | 项目CRUD + 任务CRUD + 分配任务 + 统计查看 | 本部门及下级部门 |
| 普通成员 | 查看被分配的任务、修改任务状态（仅自己的） | 仅本人数据 |

### 权限矩阵

| 权限标识 | admin | 项目经理 | 普通成员 |
|---------|:-----:|:-------:|:-------:|
| project:proj:view | ✅ | ✅ | ❌ |
| project:proj:list | ✅ | ✅ | ❌ |
| project:proj:add | ✅ | ✅ | ❌ |
| project:proj:edit | ✅ | ✅ | ❌ |
| project:proj:remove | ✅ | ❌ | ❌ |
| project:proj:export | ✅ | ✅ | ❌ |
| project:task:view | ✅ | ✅ | ✅ |
| project:task:list | ✅ | ✅ | ✅ |
| project:task:add | ✅ | ✅ | ❌ |
| project:task:edit | ✅ | ✅ | ✅（仅状态） |
| project:task:remove | ✅ | ✅ | ❌ |
| project:task:export | ✅ | ✅ | ❌ |

---

## 四、数据实体

### 4.1 项目表（proj_project）

| 列名 | 类型 | 必填 | 查询 | 查询方式 | 控件 | 字典 | 说明 |
|------|------|:----:|:----:|---------|------|------|------|
| project_id | bigint(20) | — | — | — | — | — | 主键，自增 |
| project_name | varchar(200) | ✅ | ✅ | LIKE | input | — | 项目名称 |
| project_status | char(1) | ✅ | ✅ | EQ | select | proj_project_status | 项目状态 |
| manager | varchar(64) | ✅ | ✅ | EQ | select | — | 项目负责人 |
| dept_id | bigint(20) | ✅ | — | EQ | input | — | 所属部门ID |
| priority | char(1) | ✅ | ✅ | EQ | select | proj_priority | 优先级 |
| start_date | date | — | — | — | datetime | — | 计划开始日期 |
| end_date | date | — | — | — | datetime | — | 计划结束日期 |
| description | text | — | — | — | textarea | — | 项目描述 |
| del_flag | char(1) | — | — | — | — | — | 删除标志 |

### 4.2 任务表（proj_task）

| 列名 | 类型 | 必填 | 查询 | 查询方式 | 控件 | 字典 | 说明 |
|------|------|:----:|:----:|---------|------|------|------|
| task_id | bigint(20) | — | — | — | — | — | 主键，自增 |
| task_name | varchar(200) | ✅ | ✅ | LIKE | input | — | 任务名称 |
| project_id | bigint(20) | ✅ | ✅ | EQ | select | — | 所属项目ID |
| task_status | char(1) | ✅ | ✅ | EQ | select | proj_task_status | 任务状态 |
| priority | char(1) | ✅ | ✅ | EQ | select | proj_priority | 优先级 |
| assignee | varchar(64) | ✅ | ✅ | EQ | select | — | 负责人 |
| due_date | date | — | ✅ | BETWEEN | datetime | — | 截止日期 |
| complete_date | date | — | — | — | datetime | — | 完成日期 |
| description | text | — | — | — | textarea | — | 任务描述 |
| del_flag | char(1) | — | — | — | — | — | 删除标志 |

---

## 五、RuoYi适配分析

| # | 适配项 | 结论 |
|---|--------|------|
| 1 | 功能重叠 | 5项直接复用（用户/角色/部门/操作日志/数据监控），3项需少量配置（菜单/字典/Excel导出） |
| 2 | 角色复用 | admin直接复用，需新建"项目经理"和"普通成员"角色 |
| 3 | 字典字段 | 新建3个字典：proj_project_status、proj_task_status、proj_priority |
| 4 | 数据权限 | proj_project按dept_id隔离（@DataScope），proj_task按assignee过滤 |
| 5 | 树形结构 | 无，全部继承BaseEntity |
| 6 | 主子表 | proj_project 1:N proj_task，采用独立菜单方案（非嵌套sub模板） |
| 7 | 特殊能力 | Excel导出（✅需要）、定时任务（❌不需要）、文件上传（❌不需要） |

---

## 六、完整性评估

| # | 维度 | 状态 | 说明 |
|---|------|------|------|
| 1 | 角色与权限 | ✅ 完整 | 权限矩阵已定义 |
| 2 | 业务流程 | ✅ 完整 | 正常流程+异常流程已覆盖 |
| 3 | 数据字段 | ✅ 完整 | 19个业务字段全部定义 |
| 4 | UI交互 | ⚠️ 部分缺失 | 超期标红逻辑和状态修改交互待确认 |
| 5 | 关联关系 | ✅ 完整 | 1:N关系和级联规则已定义 |
| 6 | 字典枚举 | ✅ 完整 | 3个字典共12个枚举值 |
| 7 | 安全性 | ✅ 完整 | @Xss + @DataScope + MyBatis参数化 |
| 8 | 性能 | ✅ 完整 | 数据量小，标准索引 |
| 9 | 兼容性 | ✅ 完整 | PC端主流浏览器 |
| 10 | 可扩展性 | ⚠️ 建议 | 预留任务评论/附件扩展 |

**完整度：8/10，无严重缺失**

---

## 七、待确认事项

| # | 问题 | 优先级 | 默认处理方案 |
|---|------|--------|------------|
| 1 | 超期任务标红判定逻辑 | 中 | due_date < 当前日期 且 task_status IN ('0','1') |
| 2 | 任务状态修改交互方式 | 低 | 列表操作列下拉按钮直接切换 |
| 3 | 一期是否需要任务评论 | 低 | 一期不做，预留扩展 |

---

## 八、结论

**✅ 需求完整，可以进入第二步——数据库设计与代码生成阶段。**

- 完整度评分：**8/10**
- 阻塞项：**无**
- 待确认项：3项（均为低/中优先级，可使用默认方案先行开发）
- 预计开发工作量：
  - 数据库表：2张（proj_project, proj_task）
  - 字典类型：3个（12个枚举值）
  - 菜单：1个一级目录 + 2个二级菜单 + 12个按钮权限
  - 角色：2个新角色
  - Java代码：Controller×2, Service×2, Mapper×2, Domain×2
  - 前端页面：4个HTML（项目列表/新增编辑, 任务列表/新增编辑）
