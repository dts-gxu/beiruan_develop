# 步骤1.4 — RuoYi适配分析报告

> 基于步骤1.3的关键信息提取结果  
> 分析日期：2026-03-16  

---

## 1. 功能重叠检查

| RuoYi内置功能 | 是否重叠 | 说明 |
|--------------|---------|------|
| 用户管理 | ✅ 直接复用 | 项目负责人(manager)和任务负责人(assignee)通过login_name关联sys_user，无需重建用户体系 |
| 角色管理 | ✅ 直接复用 | 新模块菜单纳入现有角色体系，通过角色-菜单关联控制权限 |
| 部门管理 | ✅ 直接复用 | 项目通过dept_id关联sys_dept，@DataScope控制数据权限 |
| 菜单管理 | ⚠️ 需新增 | 需要为项目管理模块生成菜单SQL（一级目录+两个二级菜单+按钮权限） |
| 字典管理 | ⚠️ 需新增 | 需要新增3个字典类型：proj_project_status、proj_task_status、proj_priority |
| 操作日志 | ✅ 直接复用 | Controller方法加@Log注解即可自动记录操作日志 |
| 数据监控 | ✅ 直接复用 | Druid监控自动覆盖新增SQL |
| 文件上传 | ❌ 不需要 | 本模块无文件上传需求 |
| 定时任务 | ❌ 不需要 | 超期提醒用前端列表标红实现，无需后台定时任务 |
| Excel导出 | ⚠️ 需开发 | 项目列表和任务列表都需要导出功能，使用@Excel注解+ExcelUtil |
| 代码生成 | ✅ 可参考 | 可先用RuoYi代码生成器生成基础CRUD，再手动增加业务逻辑 |

**结论**：18个内置功能中，5个直接复用，3个需要少量配置，2个不需要。**无需从零开发任何基础设施**。

---

## 2. 角色复用

| 需求角色 | 映射方案 | 操作 |
|---------|---------|------|
| 超级管理员 | → 直接复用admin角色（role_id=1） | 无需操作，admin自动拥有全部权限 |
| 项目经理 | → 新建角色"项目经理"，或复用common角色并分配菜单权限 | 新建角色，分配项目管理+任务管理菜单权限，数据范围设为"本部门及以下" |
| 普通成员 | → 需要新建角色"项目成员" | 新建角色，仅分配任务查看+任务状态修改权限，数据范围设为"仅本人" |

**具体权限分配矩阵**：

| 权限 | admin | 项目经理 | 普通成员 |
|------|:-----:|:-------:|:-------:|
| project:proj:view | ✅ | ✅ | ❌ |
| project:proj:list | ✅ | ✅ | ❌ |
| project:proj:add | ✅ | ✅ | ❌ |
| project:proj:edit | ✅ | ✅ | ❌ |
| project:proj:remove | ✅ | ❌ | ❌ |
| project:proj:export | ✅ | ✅ | ❌ |
| project:task:view | ✅ | ✅ | ✅ |
| project:task:list | ✅ | ✅ | ✅ |
| project:task:add | ✅ | ✅ | ❌ |
| project:task:edit | ✅ | ✅ | ✅（仅状态字段） |
| project:task:remove | ✅ | ✅ | ❌ |
| project:task:export | ✅ | ✅ | ❌ |

---

## 3. 字典字段识别

| 字段 | 建议字典类型 | 复用/新建 | 枚举值 |
|------|------------|----------|--------|
| project_status | proj_project_status | 新建 | 0=筹备中(info), 1=进行中(primary), 2=已完成(success), 3=已搁置(warning) |
| task_status | proj_task_status | 新建 | 0=待处理(info), 1=进行中(primary), 2=已完成(success), 3=已关闭(default) |
| priority | proj_priority | 新建 | 0=低(info), 1=中(primary), 2=高(warning), 3=紧急(danger) |
| del_flag | — | 框架内置 | 0=正常, 2=删除（无需处理，框架自动管理） |

---

## 4. 数据权限

| 实体 | 需要数据权限 | 过滤字段 | 范围类型 | 实现方式 |
|------|:-----------:|---------|---------|---------|
| proj_project | ✅ | dept_id | dept / deptAndChild | Mapper接口加@DataScope(deptAlias = "p") |
| proj_task | ⚠️ 间接 | — | — | 通过JOIN proj_project间接过滤，或按assignee=当前用户过滤 |

**proj_project的数据权限实现**：
```java
@DataScope(deptAlias = "p")
public List<ProjProject> selectProjectList(ProjProject project) { ... }
```

**proj_task的数据权限实现**（普通成员只看自己的任务）：
```java
// 方案：在Service层判断角色，如果是普通成员，自动加WHERE assignee = #{loginName}
```

---

## 5. 树形结构判断

**❌ 本模块没有树形结构。**

所有实体（proj_project、proj_task）均为扁平结构，全部继承 `BaseEntity`，不需要 `TreeEntity`。

---

## 6. 主子表关系

| 主表 | 子表 | 关系 | 外键 |
|------|------|------|------|
| proj_project | proj_task | 1:N | proj_task.project_id → proj_project.project_id |

**实现方案选择**：

- **方案A**：使用RuoYi主子表模板（tplCategory='sub'），在项目表单中嵌入任务子表编辑
  - 优点：标准模板，代码生成器直接支持
  - 缺点：任务较多时表单过长
- **方案B（推荐）**：项目和任务独立两个菜单，通过project_id关联查询
  - 优点：灵活，任务列表可独立筛选
  - 缺点：需要手动写关联查询

**推荐方案B**：两个独立CRUD菜单。在任务列表页提供"所属项目"下拉筛选。

---

## 7. 特殊能力需求

| 能力 | 是否需要 | 说明 |
|------|:-------:|------|
| 定时任务 | ❌ | 超期提醒用前端标红，无需定时任务 |
| 文件上传 | ❌ | 本模块无附件需求 |
| Excel导入 | ❌ | 暂不需要导入 |
| Excel导出 | ✅ | 项目列表和任务列表都需要导出，使用@Excel注解 |
| 数据权限 | ✅ | 项目按部门隔离，任务按负责人隔离 |

---

> ✅ 检查清单：
> - [x] 7个适配问题全部回答
> - [x] 每个问题有明确的结论（复用/新建/不需要）
> - [x] 字典字段全部识别，含枚举值
> - [x] 数据权限方案已确定
