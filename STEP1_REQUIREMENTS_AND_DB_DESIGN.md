# 第一步：需求分析与数据库功能方案设计

> 对应流程：AI_CODE_GENERATION_WORKFLOW.md 第一步 + 第二步  
> 日期：2026-03-15  
> 框架：RuoYi v4.8.2（前后端不分离）  
> 状态：待评审

---

## 一、现有系统分析

### 1.1 RuoYi已有的20张数据库表

通过分析 `sql/ry_20250416.sql` 和 `sql/quartz.sql`，当前数据库 `ruoyi` 中已有以下表：

| # | 表名 | 说明 | 类别 |
|---|------|------|------|
| 1 | sys_dept | 部门表（树形结构） | 组织架构 |
| 2 | sys_user | 用户信息表 | 用户体系 |
| 3 | sys_post | 岗位信息表 | 用户体系 |
| 4 | sys_role | 角色信息表 | 权限体系 |
| 5 | sys_menu | 菜单权限表（M目录/C菜单/F按钮） | 权限体系 |
| 6 | sys_user_role | 用户-角色关联表 | 权限体系 |
| 7 | sys_role_menu | 角色-菜单关联表 | 权限体系 |
| 8 | sys_role_dept | 角色-部门关联表（数据权限） | 权限体系 |
| 9 | sys_user_post | 用户-岗位关联表 | 用户体系 |
| 10 | sys_oper_log | 操作日志记录 | 日志 |
| 11 | sys_dict_type | 字典类型表 | 数据字典 |
| 12 | sys_dict_data | 字典数据表 | 数据字典 |
| 13 | sys_config | 参数配置表 | 系统配置 |
| 14 | sys_logininfor | 系统访问记录 | 日志 |
| 15 | sys_user_online | 在线用户记录 | 监控 |
| 16 | sys_job | 定时任务调度表 | 定时任务 |
| 17 | sys_job_log | 定时任务调度日志表 | 定时任务 |
| 18 | sys_notice | 通知公告表 | 通知 |
| 19 | gen_table | 代码生成业务表 | 工具 |
| 20 | gen_table_column | 代码生成业务表字段 | 工具 |
| 21-31 | qrtz_* | Quartz定时任务引擎表（11张） | 定时任务 |

### 1.2 已有菜单层级结构

```
系统管理(M, id=1)
├── 用户管理(C, id=100)  → 7个按钮权限(F)：查询/新增/修改/删除/导出/导入/重置密码
├── 角色管理(C, id=101)  → 5个按钮权限(F)
├── 菜单管理(C, id=102)  → 4个按钮权限(F)
├── 部门管理(C, id=103)  → 4个按钮权限(F)
├── 岗位管理(C, id=104)  → 5个按钮权限(F)
├── 字典管理(C, id=105)  → 5个按钮权限(F)
├── 参数设置(C, id=106)  → 5个按钮权限(F)
├── 通知公告(C, id=107)  → 4个按钮权限(F)
└── 日志管理(M, id=108)
    ├── 操作日志(C, id=500) → 4个按钮权限(F)
    └── 登录日志(C, id=501) → 4个按钮权限(F)

系统监控(M, id=2)
├── 在线用户(C, id=109)
├── 定时任务(C, id=110)
├── 数据监控(C, id=111)
├── 服务监控(C, id=112)
└── 缓存监控(C, id=113)

系统工具(M, id=3)
├── 表单构建(C, id=114)
├── 代码生成(C, id=115)
└── 系统接口(C, id=116)
```

### 1.3 已有字典类型

| dict_type | dict_name | 说明 |
|-----------|-----------|------|
| sys_user_sex | 用户性别 | 0男 1女 2未知 |
| sys_show_hide | 菜单状态 | 0显示 1隐藏 |
| sys_normal_disable | 系统开关 | 0正常 1停用（**可复用**） |
| sys_job_status | 任务状态 | 0正常 1暂停 |
| sys_job_group | 任务分组 | DEFAULT/SYSTEM |
| sys_yes_no | 系统是否 | Y是 N否（**可复用**） |
| sys_notice_type | 通知类型 | 1通知 2公告 |
| sys_notice_status | 通知状态 | 0正常 1关闭 |
| sys_oper_type | 操作类型 | 0-9各种操作 |
| sys_common_status | 系统状态 | 0成功 1失败 |

### 1.4 已有角色

| role_id | role_name | role_key | data_scope | 说明 |
|---------|-----------|----------|------------|------|
| 1 | 超级管理员 | admin | 1(全部数据) | 拥有所有权限，不受权限控制 |
| 2 | 普通角色 | common | 2(自定数据) | 拥有所有菜单权限，数据权限为自定义 |

### 1.5 已有部门组织

```
若依科技(id=100)
├── 深圳总公司(id=101)
│   ├── 研发部门(id=103)
│   ├── 市场部门(id=104)
│   ├── 测试部门(id=105)
│   ├── 财务部门(id=106)
│   └── 运维部门(id=107)
└── 长沙分公司(id=102)
    ├── 市场部门(id=108)
    └── 财务部门(id=109)
```

---

## 二、RuoYi适配分析

### 2.1 可直接复用的内置能力

| 需求 | 复用方式 | 不需要开发 |
|------|---------|-----------|
| 用户登录/注册 | Shiro内置 | ✅ |
| 用户管理（增删改查） | sys_user内置 | ✅ |
| 角色权限分配 | sys_role + sys_menu内置 | ✅ |
| 部门组织架构 | sys_dept内置（树形） | ✅ |
| 操作日志记录 | @Log注解自动记录 | ✅ |
| 数据字典管理 | sys_dict_type/data内置 | ✅ |
| 文件上传下载 | CommonController内置 | ✅ |
| Excel导入导出 | @Excel + ExcelUtil内置 | ✅ |
| 数据权限控制 | @DataScope注解内置 | ✅ |

### 2.2 新业务模块开发需要做的事

当你要新增一个业务模块（如"项目管理"），需要：

1. **设计数据库表** → 新建业务表（必须含BaseEntity字段）
2. **生成菜单SQL** → 在sys_menu中插入目录+菜单+按钮权限
3. **生成字典SQL** → 如果有枚举字段，在sys_dict中插入类型和数据
4. **生成Java代码** → Domain/Mapper/XML/Service/Controller（遵循RuoYi规范）
5. **生成HTML页面** → Thymeleaf模板（列表页/新增页/编辑页）
6. **分配角色权限** → 在角色管理中给对应角色勾选新菜单

---

## 三、示例业务需求：项目任务管理模块

> 以下以"项目任务管理"为示例，展示完整的需求分析和数据库设计。
> 你在实际开发时，替换成你自己的业务需求即可，流程和规范完全一致。

### 3.1 功能概述

开发一个**项目任务管理**模块，挂在RuoYi后台左侧菜单中，实现以下功能：

| 功能 | 说明 | 涉及操作 |
|------|------|---------|
| 项目管理 | 管理多个项目，包含名称、负责人、状态、起止日期 | CRUD + 导出 |
| 任务管理 | 每个项目下有多个任务，任务分配给人员 | CRUD + 导出 |
| 任务看板 | 按状态查看任务分布（待办/进行中/完成） | 列表查询 |

### 3.2 功能间逻辑关系

```
┌─────────────┐         ┌─────────────┐
│  项目(proj)  │ 1 ─── N │  任务(task)  │
│             │         │             │
│ project_id  │◄────────│ project_id  │
│ project_name│         │ task_name   │
│ manager     │         │ assign_user │
│ status      │         │ task_status │
└──────┬──────┘         └──────┬──────┘
       │                       │
       │ manager字段           │ assign_user字段
       │ 关联 sys_user         │ 关联 sys_user
       │ 的 login_name         │ 的 login_name
       ▼                       ▼
┌─────────────┐
│ sys_user    │  ← RuoYi内置，不需要新建
│ (login_name)│
└─────────────┘

┌─────────────────────────────────────────────┐
│  权限控制关系                                │
│                                             │
│  sys_menu (新增菜单)                         │
│    └─ project:proj:view / task:view         │
│       └─ 5个按钮权限 (list/add/edit/remove/  │
│          export)                            │
│                                             │
│  sys_role_menu (角色-菜单关联)               │
│    └─ 给角色分配新模块的菜单权限             │
│                                             │
│  sys_dict_type + sys_dict_data              │
│    └─ 项目状态(proj_project_status)          │
│    └─ 任务状态(proj_task_status)             │
│    └─ 任务优先级(proj_task_priority)         │
└─────────────────────────────────────────────┘
```

**关键逻辑约束：**
- **项目与任务是一对多关系**：一个项目下有N个任务，通过`project_id`外键关联
- **删除项目前必须检查**：如果项目下有未完成的任务，不允许删除项目
- **项目负责人和任务负责人**：都通过`login_name`关联到`sys_user`表，不创建新的用户表
- **任务状态变更影响项目**：当项目下所有任务完成时，可提示用户是否关闭项目
- **数据权限**：项目数据按部门隔离，使用`@DataScope`注解，A部门的人只能看到A部门的项目

### 3.3 用户角色定义

| 角色 | 基于 | 权限说明 |
|------|------|---------|
| 超级管理员(admin) | 已有admin角色 | 可以查看和操作所有项目和任务 |
| 项目经理 | 新建角色或复用common角色 | 可以创建项目、分配任务、管理自己部门的项目 |
| 普通成员 | 新建角色或复用common角色 | 只能查看分配给自己的任务，修改任务状态 |

> 注意：角色通过RuoYi内置的"角色管理"功能创建和分配，不需要写代码。只需要确保新模块的菜单权限SQL正确生成。

---

## 四、数据库设计

### 4.1 新增业务表

#### 4.1.1 项目表 `proj_project`

```sql
-- ----------------------------
-- 项目管理表
-- ----------------------------
DROP TABLE IF EXISTS proj_project;
CREATE TABLE proj_project (
    project_id    BIGINT(20)    NOT NULL AUTO_INCREMENT  COMMENT '项目ID',
    project_name  VARCHAR(200)  NOT NULL DEFAULT ''       COMMENT '项目名称',
    project_code  VARCHAR(50)   DEFAULT ''                COMMENT '项目编号',
    manager       VARCHAR(64)   DEFAULT ''                COMMENT '项目负责人（关联sys_user.login_name）',
    dept_id       BIGINT(20)    DEFAULT NULL              COMMENT '所属部门ID（关联sys_dept.dept_id）',
    project_status CHAR(1)      NOT NULL DEFAULT '0'      COMMENT '项目状态（0筹备中 1进行中 2已完成 3已搁置）',
    start_date    DATE                                    COMMENT '计划开始日期',
    end_date      DATE                                    COMMENT '计划结束日期',
    description   VARCHAR(2000) DEFAULT ''                COMMENT '项目描述',
    del_flag      CHAR(1)       DEFAULT '0'               COMMENT '删除标志（0存在 2删除）',
    create_by     VARCHAR(64)   DEFAULT ''                COMMENT '创建者',
    create_time   DATETIME                                COMMENT '创建时间',
    update_by     VARCHAR(64)   DEFAULT ''                COMMENT '更新者',
    update_time   DATETIME                                COMMENT '更新时间',
    remark        VARCHAR(500)  DEFAULT NULL              COMMENT '备注',
    PRIMARY KEY (project_id),
    KEY idx_proj_status (project_status),
    KEY idx_proj_manager (manager),
    KEY idx_proj_dept (dept_id)
) ENGINE=InnoDB AUTO_INCREMENT=1 COMMENT='项目管理表';
```

**字段说明：**
- `manager` → 存 `sys_user.login_name`，不建外键，通过业务逻辑关联
- `dept_id` → 存 `sys_dept.dept_id`，用于数据权限控制（@DataScope按部门过滤）
- `project_status` → 使用字典 `proj_project_status`
- `del_flag` → 软删除，与RuoYi框架保持一致
- `create_by/create_time/update_by/update_time/remark` → BaseEntity必须字段

#### 4.1.2 任务表 `proj_task`

```sql
-- ----------------------------
-- 项目任务表
-- ----------------------------
DROP TABLE IF EXISTS proj_task;
CREATE TABLE proj_task (
    task_id       BIGINT(20)    NOT NULL AUTO_INCREMENT  COMMENT '任务ID',
    project_id    BIGINT(20)    NOT NULL                  COMMENT '所属项目ID（关联proj_project.project_id）',
    task_name     VARCHAR(200)  NOT NULL DEFAULT ''       COMMENT '任务名称',
    task_status   CHAR(1)       NOT NULL DEFAULT '0'      COMMENT '任务状态（0待办 1进行中 2已完成 3已关闭）',
    priority      CHAR(1)       DEFAULT '1'               COMMENT '优先级（0低 1中 2高 3紧急）',
    assign_user   VARCHAR(64)   DEFAULT ''                COMMENT '负责人（关联sys_user.login_name）',
    start_date    DATE                                    COMMENT '计划开始日期',
    due_date      DATE                                    COMMENT '截止日期',
    finish_date   DATE                                    COMMENT '实际完成日期',
    description   VARCHAR(2000) DEFAULT ''                COMMENT '任务描述',
    del_flag      CHAR(1)       DEFAULT '0'               COMMENT '删除标志（0存在 2删除）',
    create_by     VARCHAR(64)   DEFAULT ''                COMMENT '创建者',
    create_time   DATETIME                                COMMENT '创建时间',
    update_by     VARCHAR(64)   DEFAULT ''                COMMENT '更新者',
    update_time   DATETIME                                COMMENT '更新时间',
    remark        VARCHAR(500)  DEFAULT NULL              COMMENT '备注',
    PRIMARY KEY (task_id),
    KEY idx_task_project (project_id),
    KEY idx_task_status (task_status),
    KEY idx_task_assign (assign_user),
    KEY idx_task_due_date (due_date)
) ENGINE=InnoDB AUTO_INCREMENT=1 COMMENT='项目任务表';
```

**字段说明：**
- `project_id` → 外键关联 `proj_project.project_id`（逻辑外键，不加物理约束）
- `task_status` → 使用字典 `proj_task_status`
- `priority` → 使用字典 `proj_task_priority`
- `assign_user` → 存 `sys_user.login_name`

### 4.2 ER图

```
┌──────────────────┐       ┌──────────────────┐       ┌──────────────────┐
│    sys_dept      │       │   sys_user       │       │  sys_dict_data   │
│ (RuoYi内置)      │       │ (RuoYi内置)      │       │ (RuoYi内置)      │
├──────────────────┤       ├──────────────────┤       ├──────────────────┤
│ dept_id (PK)     │       │ user_id (PK)     │       │ dict_code (PK)   │
│ dept_name        │       │ login_name (UK)  │       │ dict_type        │
│ parent_id        │       │ user_name        │       │ dict_label       │
│ ancestors        │       │ dept_id (FK)     │       │ dict_value       │
└────────┬─────────┘       └───┬──────┬───────┘       └──────────────────┘
         │                     │      │
         │ dept_id             │      │ login_name
         │                     │      │
┌────────▼─────────┐           │      │
│  proj_project    │◄──────────┘      │
│ (新建)            │ manager          │
├──────────────────┤                  │
│ project_id (PK)  │                  │
│ project_name     │                  │
│ project_code     │                  │
│ manager          │──────────────────┘
│ dept_id          │
│ project_status   │──→ 字典: proj_project_status
│ start_date       │
│ end_date         │
│ description      │
│ BaseEntity字段    │
└────────┬─────────┘
         │
         │ project_id (1:N)
         │
┌────────▼─────────┐
│  proj_task       │
│ (新建)            │
├──────────────────┤
│ task_id (PK)     │
│ project_id (FK)  │──→ proj_project.project_id
│ task_name        │
│ task_status      │──→ 字典: proj_task_status
│ priority         │──→ 字典: proj_task_priority
│ assign_user      │──→ sys_user.login_name
│ start_date       │
│ due_date         │
│ finish_date      │
│ description      │
│ BaseEntity字段    │
└──────────────────┘
```

### 4.3 菜单权限SQL

```sql
-- =============================================
-- 菜单权限SQL（必须执行）
-- =============================================

-- 一级目录：项目管理
INSERT INTO sys_menu (menu_name, parent_id, order_num, url, menu_type, visible, perms, icon, create_by, create_time)
VALUES ('项目管理', '0', '5', '#', 'M', '0', '', 'fa fa-briefcase', 'admin', sysdate());
SELECT @parentId := LAST_INSERT_ID();

-- 二级菜单：项目信息
INSERT INTO sys_menu (menu_name, parent_id, order_num, url, menu_type, visible, perms, icon, create_by, create_time)
VALUES ('项目信息', @parentId, '1', '/project/proj', 'C', '0', 'project:proj:view', 'fa fa-folder-open', 'admin', sysdate());
SELECT @projMenuId := LAST_INSERT_ID();

-- 项目信息 - 按钮权限（5个标准按钮）
INSERT INTO sys_menu (menu_name, parent_id, order_num, url, menu_type, visible, perms, icon, create_by, create_time) VALUES
('项目查询', @projMenuId, '1', '#', 'F', '0', 'project:proj:list',   '#', 'admin', sysdate()),
('项目新增', @projMenuId, '2', '#', 'F', '0', 'project:proj:add',    '#', 'admin', sysdate()),
('项目修改', @projMenuId, '3', '#', 'F', '0', 'project:proj:edit',   '#', 'admin', sysdate()),
('项目删除', @projMenuId, '4', '#', 'F', '0', 'project:proj:remove', '#', 'admin', sysdate()),
('项目导出', @projMenuId, '5', '#', 'F', '0', 'project:proj:export', '#', 'admin', sysdate());

-- 二级菜单：任务管理
INSERT INTO sys_menu (menu_name, parent_id, order_num, url, menu_type, visible, perms, icon, create_by, create_time)
VALUES ('任务管理', @parentId, '2', '/project/task', 'C', '0', 'project:task:view', 'fa fa-tasks', 'admin', sysdate());
SELECT @taskMenuId := LAST_INSERT_ID();

-- 任务管理 - 按钮权限（5个标准按钮）
INSERT INTO sys_menu (menu_name, parent_id, order_num, url, menu_type, visible, perms, icon, create_by, create_time) VALUES
('任务查询', @taskMenuId, '1', '#', 'F', '0', 'project:task:list',   '#', 'admin', sysdate()),
('任务新增', @taskMenuId, '2', '#', 'F', '0', 'project:task:add',    '#', 'admin', sysdate()),
('任务修改', @taskMenuId, '3', '#', 'F', '0', 'project:task:edit',   '#', 'admin', sysdate()),
('任务删除', @taskMenuId, '4', '#', 'F', '0', 'project:task:remove', '#', 'admin', sysdate()),
('任务导出', @taskMenuId, '5', '#', 'F', '0', 'project:task:export', '#', 'admin', sysdate());
```

**菜单层级关系：**
```
项目管理(M, order=5)             ← 新的一级目录，排在系统工具后面
├── 项目信息(C)                  ← URL: /project/proj
│   ├── 项目查询(F) → project:proj:list
│   ├── 项目新增(F) → project:proj:add
│   ├── 项目修改(F) → project:proj:edit
│   ├── 项目删除(F) → project:proj:remove
│   └── 项目导出(F) → project:proj:export
└── 任务管理(C)                  ← URL: /project/task
    ├── 任务查询(F) → project:task:list
    ├── 任务新增(F) → project:task:add
    ├── 任务修改(F) → project:task:edit
    ├── 任务删除(F) → project:task:remove
    └── 任务导出(F) → project:task:export
```

### 4.4 字典数据SQL

```sql
-- =============================================
-- 字典数据SQL（必须执行）
-- =============================================

-- 字典类型：项目状态
INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
VALUES ('项目状态', 'proj_project_status', '0', 'admin', sysdate(), '项目管理-项目状态');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time) VALUES
(0, '筹备中', '0', 'proj_project_status', '', 'info',    'Y', '0', 'admin', sysdate()),
(1, '进行中', '1', 'proj_project_status', '', 'primary', 'N', '0', 'admin', sysdate()),
(2, '已完成', '2', 'proj_project_status', '', 'success', 'N', '0', 'admin', sysdate()),
(3, '已搁置', '3', 'proj_project_status', '', 'warning', 'N', '0', 'admin', sysdate());

-- 字典类型：任务状态
INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
VALUES ('任务状态', 'proj_task_status', '0', 'admin', sysdate(), '项目管理-任务状态');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time) VALUES
(0, '待办',   '0', 'proj_task_status', '', 'info',    'Y', '0', 'admin', sysdate()),
(1, '进行中', '1', 'proj_task_status', '', 'primary', 'N', '0', 'admin', sysdate()),
(2, '已完成', '2', 'proj_task_status', '', 'success', 'N', '0', 'admin', sysdate()),
(3, '已关闭', '3', 'proj_task_status', '', 'danger',  'N', '0', 'admin', sysdate());

-- 字典类型：任务优先级
INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
VALUES ('任务优先级', 'proj_task_priority', '0', 'admin', sysdate(), '项目管理-任务优先级');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time) VALUES
(0, '低',   '0', 'proj_task_priority', '', 'info',    'N', '0', 'admin', sysdate()),
(1, '中',   '1', 'proj_task_priority', '', 'primary', 'Y', '0', 'admin', sysdate()),
(2, '高',   '2', 'proj_task_priority', '', 'warning', 'N', '0', 'admin', sysdate()),
(3, '紧急', '3', 'proj_task_priority', '', 'danger',  'N', '0', 'admin', sysdate());
```

---

## 五、功能方案详细设计

### 5.1 项目信息管理

#### 页面设计

**列表页** `/project/proj`

| 搜索条件 | 类型 | 查询方式 |
|---------|------|---------|
| 项目名称 | 文本输入框 | LIKE模糊查询 |
| 项目状态 | 下拉选择（字典proj_project_status） | EQ精确匹配 |
| 负责人 | 文本输入框 | EQ精确匹配 |
| 创建时间 | 日期范围 | BETWEEN |

| 列表字段 | 是否排序 | 说明 |
|---------|---------|------|
| 项目ID | 否 | 隐藏列 |
| 项目名称 | 否 | 可点击进入详情 |
| 项目编号 | 否 | |
| 负责人 | 否 | |
| 项目状态 | 否 | 字典翻译显示标签 |
| 开始日期 | 是 | |
| 结束日期 | 是 | |
| 创建时间 | 是 | |
| 操作 | 否 | 编辑/删除按钮 |

**工具栏按钮**（受shiro:hasPermission控制）：添加、修改、删除、导出

**新增/编辑页**

| 字段 | 控件类型 | 验证规则 | 说明 |
|------|---------|---------|------|
| 项目名称 | 文本输入 | 必填，最大200字 | @Xss防护 |
| 项目编号 | 文本输入 | 最大50字 | 可选 |
| 负责人 | 下拉选择（用户列表） | 必填 | 从sys_user获取 |
| 所属部门 | 部门树选择 | 必填 | 从sys_dept获取 |
| 项目状态 | 下拉选择（字典） | 必填 | |
| 开始日期 | 日期选择器 | | class="time-input" |
| 结束日期 | 日期选择器 | | 必须>=开始日期 |
| 项目描述 | 文本域textarea | 最大2000字 | |
| 备注 | 文本域textarea | 最大500字 | BaseEntity字段 |

#### 业务逻辑

```
新增项目：
  1. 验证项目名称不为空
  2. 设置createBy为当前登录用户(getLoginName())
  3. 设置createTime
  4. 插入proj_project表

删除项目：
  1. 检查该项目下是否有未完成的任务（task_status != '2' AND task_status != '3'）
  2. 如果有未完成任务 → 返回错误："该项目下有N个未完成的任务，无法删除"
  3. 如果没有 → 软删除（设置del_flag='2'）
  4. 同时软删除该项目下的所有任务

修改项目状态：
  1. 如果从"进行中"改为"已完成"：
     → 检查是否有未完成的任务，如果有则提示警告
  2. 设置updateBy和updateTime
```

#### Controller方法清单

| 方法 | URL | HTTP | 权限 | @Log | 说明 |
|------|-----|------|------|------|------|
| proj() | /project/proj | GET | project:proj:view | 否 | 返回列表页视图 |
| list() | /project/proj/list | POST | project:proj:list | 否 | 分页查询，返回TableDataInfo |
| add() | /project/proj/add | GET | project:proj:add | 否 | 返回新增页视图 |
| addSave() | /project/proj/add | POST | project:proj:add | INSERT | 保存新增，返回AjaxResult |
| edit() | /project/proj/edit/{id} | GET | project:proj:edit | 否 | 返回编辑页视图 |
| editSave() | /project/proj/edit | POST | project:proj:edit | UPDATE | 保存修改，返回AjaxResult |
| remove() | /project/proj/remove | POST | project:proj:remove | DELETE | 删除，返回AjaxResult |
| export() | /project/proj/export | POST | project:proj:export | EXPORT | 导出Excel |

### 5.2 任务管理

#### 页面设计

**列表页** `/project/task`

| 搜索条件 | 类型 | 查询方式 |
|---------|------|---------|
| 所属项目 | 下拉选择（项目列表） | EQ |
| 任务名称 | 文本输入框 | LIKE |
| 任务状态 | 下拉选择（字典proj_task_status） | EQ |
| 负责人 | 文本输入框 | EQ |
| 优先级 | 下拉选择（字典proj_task_priority） | EQ |
| 截止日期 | 日期范围 | BETWEEN |

| 列表字段 | 说明 |
|---------|------|
| 任务ID | 隐藏列 |
| 所属项目 | 显示project_name（需JOIN或子查询） |
| 任务名称 | |
| 任务状态 | 字典翻译 |
| 优先级 | 字典翻译 |
| 负责人 | |
| 截止日期 | 超期标红显示 |
| 创建时间 | |
| 操作 | 编辑/删除 |

**新增/编辑页**

| 字段 | 控件类型 | 验证规则 | 说明 |
|------|---------|---------|------|
| 所属项目 | 下拉选择（项目列表） | 必填 | 从proj_project获取 |
| 任务名称 | 文本输入 | 必填，最大200字 | @Xss防护 |
| 任务状态 | 下拉选择（字典） | 必填 | |
| 优先级 | 下拉选择（字典） | 必填 | |
| 负责人 | 下拉选择（用户列表） | 必填 | 从sys_user获取 |
| 开始日期 | 日期选择器 | | |
| 截止日期 | 日期选择器 | | 必须>=开始日期 |
| 任务描述 | 文本域textarea | 最大2000字 | |
| 备注 | 文本域textarea | 最大500字 | |

#### 业务逻辑

```
新增任务：
  1. 验证所属项目ID存在且未删除
  2. 验证任务名称不为空
  3. 设置createBy为当前登录用户
  4. 插入proj_task表

修改任务状态为"已完成"：
  1. 设置finish_date为当前日期
  2. 检查该项目下是否所有任务都已完成/已关闭
  3. 如果是 → 提示用户"所有任务已完成，是否关闭项目？"（前端确认框）

删除任务：
  1. 软删除（设置del_flag='2'）
  2. 只有"待办"和"已关闭"状态的任务允许删除
  3. "进行中"状态的任务不允许直接删除，需先关闭

查询任务列表：
  1. 支持按项目筛选
  2. 列表中显示项目名称（需要关联查询proj_project表）
  3. 截止日期已过期且任务未完成的，前端标红显示
```

#### Controller方法清单

| 方法 | URL | HTTP | 权限 | @Log | 说明 |
|------|-----|------|------|------|------|
| task() | /project/task | GET | project:task:view | 否 | 返回列表页视图 |
| list() | /project/task/list | POST | project:task:list | 否 | 分页查询 |
| add() | /project/task/add | GET | project:task:add | 否 | 返回新增页视图 |
| addSave() | /project/task/add | POST | project:task:add | INSERT | 保存新增 |
| edit() | /project/task/edit/{id} | GET | project:task:edit | 否 | 返回编辑页视图 |
| editSave() | /project/task/edit | POST | project:task:edit | UPDATE | 保存修改 |
| remove() | /project/task/remove | POST | project:task:remove | DELETE | 删除 |
| export() | /project/task/export | POST | project:task:export | EXPORT | 导出Excel |

---

## 六、代码文件放置位置

```
ruoyi-system/src/main/java/com/ruoyi/project/
├── domain/
│   ├── ProjProject.java          ← extends BaseEntity
│   └── ProjTask.java             ← extends BaseEntity
├── mapper/
│   ├── ProjProjectMapper.java
│   └── ProjTaskMapper.java
└── service/
    ├── IProjProjectService.java
    ├── IProjTaskService.java
    └── impl/
        ├── ProjProjectServiceImpl.java
        └── ProjTaskServiceImpl.java

ruoyi-system/src/main/resources/mapper/project/
├── ProjProjectMapper.xml
└── ProjTaskMapper.xml

ruoyi-admin/src/main/java/com/ruoyi/web/controller/project/
├── ProjProjectController.java    ← extends BaseController
└── ProjTaskController.java       ← extends BaseController

ruoyi-admin/src/main/resources/templates/project/
├── proj/
│   ├── proj.html                 ← 项目列表页
│   ├── add.html                  ← 项目新增页
│   └── edit.html                 ← 项目编辑页
└── task/
    ├── task.html                 ← 任务列表页
    ├── add.html                  ← 任务新增页
    └── edit.html                 ← 任务编辑页
```

---

## 七、SQL执行顺序

当你准备部署这个模块时，SQL脚本必须按以下顺序执行：

```
1. create_table.sql    ← 先建表（proj_project、proj_task）
2. dict.sql            ← 再插入字典数据（3个字典类型 + 12条字典数据）
3. menu.sql            ← 最后插入菜单（1个目录 + 2个菜单 + 10个按钮）
```

执行完成后：
- 在RuoYi后台 → 角色管理 → 编辑角色 → 勾选"项目管理"菜单 → 保存
- 刷新页面，左侧菜单就会出现"项目管理"

---

## 八、与现有系统的集成点总结

| 集成点 | 关联表 | 关联方式 | 说明 |
|--------|-------|---------|------|
| 项目负责人 | sys_user | manager → login_name | 下拉选择用户 |
| 任务负责人 | sys_user | assign_user → login_name | 下拉选择用户 |
| 所属部门 | sys_dept | dept_id → dept_id | 部门树选择 |
| 数据权限 | sys_role_dept | @DataScope注解 | 按部门过滤项目 |
| 菜单权限 | sys_menu | @RequiresPermissions | 按钮级别权限控制 |
| 角色分配 | sys_role_menu | 角色管理界面勾选 | 不需要写代码 |
| 项目状态 | sys_dict_data | proj_project_status | 字典管理维护 |
| 任务状态 | sys_dict_data | proj_task_status | 字典管理维护 |
| 优先级 | sys_dict_data | proj_task_priority | 字典管理维护 |
| 操作日志 | sys_oper_log | @Log注解自动记录 | 不需要写代码 |

---

## 九、待确认事项

在进入代码开发阶段前，请确认以下事项：

- [ ] 项目管理是否需要文件附件功能？（如需要，复用RuoYi内置文件上传）
- [ ] 任务是否需要评论/沟通功能？（如需要，新增proj_task_comment表）
- [ ] 是否需要任务工时记录？（如需要，新增proj_task_worklog表）
- [ ] 数据权限范围确认：项目按部门隔离是否满足需求？
- [ ] 是否需要项目统计仪表板？（任务完成率、项目进度等）
- [ ] 部门名称和组织架构是否需要修改？（当前是若依科技/深圳总公司/长沙分公司）

确认后即可进入 AI_CODE_GENERATION_WORKFLOW.md 的第三步（设计评审）和第四步（AI生成代码）。
