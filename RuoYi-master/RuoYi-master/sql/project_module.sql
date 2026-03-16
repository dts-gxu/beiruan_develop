-- ----------------------------
-- 项目任务管理模块 - 完整SQL
-- 包含：建表DDL、字典数据、菜单权限
-- 执行顺序：直接全部执行即可
-- ----------------------------

-- ----------------------------
-- 1. 项目表 proj_project
-- ----------------------------
DROP TABLE IF EXISTS proj_task;
DROP TABLE IF EXISTS proj_project;

CREATE TABLE proj_project (
  project_id   bigint(20)    NOT NULL AUTO_INCREMENT  COMMENT '项目ID',
  project_name varchar(200)  NOT NULL DEFAULT ''      COMMENT '项目名称',
  project_status char(1)     NOT NULL DEFAULT '0'     COMMENT '项目状态（0筹备中 1进行中 2已完成 3已搁置）',
  manager      varchar(64)   NOT NULL DEFAULT ''      COMMENT '项目负责人',
  dept_id      bigint(20)    DEFAULT NULL             COMMENT '所属部门ID',
  priority     char(1)       NOT NULL DEFAULT '1'     COMMENT '优先级（0低 1中 2高 3紧急）',
  start_date   date          DEFAULT NULL             COMMENT '计划开始日期',
  end_date     date          DEFAULT NULL             COMMENT '计划结束日期',
  description  text                                   COMMENT '项目描述',
  del_flag     char(1)       DEFAULT '0'              COMMENT '删除标志（0正常 2删除）',
  create_by    varchar(64)   DEFAULT ''               COMMENT '创建者',
  create_time  datetime                               COMMENT '创建时间',
  update_by    varchar(64)   DEFAULT ''               COMMENT '更新者',
  update_time  datetime                               COMMENT '更新时间',
  remark       varchar(500)  DEFAULT NULL             COMMENT '备注',
  PRIMARY KEY (project_id),
  KEY idx_project_status (project_status),
  KEY idx_project_manager (manager),
  KEY idx_project_dept (dept_id)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='项目管理表';

-- ----------------------------
-- 2. 任务表 proj_task
-- ----------------------------
CREATE TABLE proj_task (
  task_id       bigint(20)    NOT NULL AUTO_INCREMENT  COMMENT '任务ID',
  task_name     varchar(200)  NOT NULL DEFAULT ''      COMMENT '任务名称',
  project_id    bigint(20)    NOT NULL                 COMMENT '所属项目ID',
  task_status   char(1)       NOT NULL DEFAULT '0'     COMMENT '任务状态（0待处理 1进行中 2已完成 3已关闭）',
  priority      char(1)       NOT NULL DEFAULT '1'     COMMENT '优先级（0低 1中 2高 3紧急）',
  assignee      varchar(64)   NOT NULL DEFAULT ''      COMMENT '负责人',
  due_date      date          DEFAULT NULL             COMMENT '截止日期',
  complete_date date          DEFAULT NULL             COMMENT '实际完成日期',
  description   text                                   COMMENT '任务描述',
  del_flag      char(1)       DEFAULT '0'              COMMENT '删除标志（0正常 2删除）',
  create_by     varchar(64)   DEFAULT ''               COMMENT '创建者',
  create_time   datetime                               COMMENT '创建时间',
  update_by     varchar(64)   DEFAULT ''               COMMENT '更新者',
  update_time   datetime                               COMMENT '更新时间',
  remark        varchar(500)  DEFAULT NULL             COMMENT '备注',
  PRIMARY KEY (task_id),
  KEY idx_task_project (project_id),
  KEY idx_task_status (task_status),
  KEY idx_task_assignee (assignee),
  KEY idx_task_due_date (due_date),
  CONSTRAINT fk_task_project FOREIGN KEY (project_id) REFERENCES proj_project (project_id)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='项目任务表';

-- ----------------------------
-- 3. 字典类型
-- ----------------------------
INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
VALUES ('项目状态', 'proj_project_status', '0', 'admin', sysdate(), '项目管理模块-项目状态');

INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
VALUES ('任务状态', 'proj_task_status', '0', 'admin', sysdate(), '项目管理模块-任务状态');

INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
VALUES ('优先级', 'proj_priority', '0', 'admin', sysdate(), '项目管理模块-优先级');

-- ----------------------------
-- 4. 字典数据 - 项目状态
-- ----------------------------
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
VALUES (1, '筹备中', '0', 'proj_project_status', '', 'info',    'Y', '0', 'admin', sysdate(), '');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
VALUES (2, '进行中', '1', 'proj_project_status', '', 'primary', 'N', '0', 'admin', sysdate(), '');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
VALUES (3, '已完成', '2', 'proj_project_status', '', 'success', 'N', '0', 'admin', sysdate(), '');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
VALUES (4, '已搁置', '3', 'proj_project_status', '', 'warning', 'N', '0', 'admin', sysdate(), '');

-- ----------------------------
-- 5. 字典数据 - 任务状态
-- ----------------------------
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
VALUES (1, '待处理', '0', 'proj_task_status', '', 'info',    'Y', '0', 'admin', sysdate(), '');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
VALUES (2, '进行中', '1', 'proj_task_status', '', 'primary', 'N', '0', 'admin', sysdate(), '');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
VALUES (3, '已完成', '2', 'proj_task_status', '', 'success', 'N', '0', 'admin', sysdate(), '');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
VALUES (4, '已关闭', '3', 'proj_task_status', '', 'default', 'N', '0', 'admin', sysdate(), '');

-- ----------------------------
-- 6. 字典数据 - 优先级
-- ----------------------------
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
VALUES (1, '低',  '0', 'proj_priority', '', 'info',    'N', '0', 'admin', sysdate(), '');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
VALUES (2, '中',  '1', 'proj_priority', '', 'primary', 'Y', '0', 'admin', sysdate(), '');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
VALUES (3, '高',  '2', 'proj_priority', '', 'warning', 'N', '0', 'admin', sysdate(), '');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
VALUES (4, '紧急', '3', 'proj_priority', '', 'danger',  'N', '0', 'admin', sysdate(), '');

-- ----------------------------
-- 7. 菜单 - 一级目录：项目管理
-- ----------------------------
INSERT INTO sys_menu (menu_name, parent_id, order_num, url, menu_type, visible, is_refresh, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('项目管理', 0, 5, '#', 'M', '0', '1', '', 'fa fa-briefcase', 'admin', sysdate(), '', null, '项目管理目录');

SET @projectMenuId = LAST_INSERT_ID();

-- 二级菜单：项目列表
INSERT INTO sys_menu (menu_name, parent_id, order_num, url, menu_type, visible, is_refresh, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('项目列表', @projectMenuId, 1, '/project/proj', 'C', '0', '1', 'project:proj:view', 'fa fa-folder-open', 'admin', sysdate(), '', null, '');

SET @projMenuId = LAST_INSERT_ID();

-- 项目列表按钮权限
INSERT INTO sys_menu (menu_name, parent_id, order_num, url, menu_type, visible, is_refresh, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('项目查询', @projMenuId, 1, '#', 'F', '0', '1', 'project:proj:list', '#', 'admin', sysdate(), '', null, '');
INSERT INTO sys_menu (menu_name, parent_id, order_num, url, menu_type, visible, is_refresh, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('项目新增', @projMenuId, 2, '#', 'F', '0', '1', 'project:proj:add', '#', 'admin', sysdate(), '', null, '');
INSERT INTO sys_menu (menu_name, parent_id, order_num, url, menu_type, visible, is_refresh, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('项目修改', @projMenuId, 3, '#', 'F', '0', '1', 'project:proj:edit', '#', 'admin', sysdate(), '', null, '');
INSERT INTO sys_menu (menu_name, parent_id, order_num, url, menu_type, visible, is_refresh, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('项目删除', @projMenuId, 4, '#', 'F', '0', '1', 'project:proj:remove', '#', 'admin', sysdate(), '', null, '');
INSERT INTO sys_menu (menu_name, parent_id, order_num, url, menu_type, visible, is_refresh, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('项目导出', @projMenuId, 5, '#', 'F', '0', '1', 'project:proj:export', '#', 'admin', sysdate(), '', null, '');

-- 二级菜单：任务列表
INSERT INTO sys_menu (menu_name, parent_id, order_num, url, menu_type, visible, is_refresh, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('任务列表', @projectMenuId, 2, '/project/task', 'C', '0', '1', 'project:task:view', 'fa fa-tasks', 'admin', sysdate(), '', null, '');

SET @taskMenuId = LAST_INSERT_ID();

-- 任务列表按钮权限
INSERT INTO sys_menu (menu_name, parent_id, order_num, url, menu_type, visible, is_refresh, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('任务查询', @taskMenuId, 1, '#', 'F', '0', '1', 'project:task:list', '#', 'admin', sysdate(), '', null, '');
INSERT INTO sys_menu (menu_name, parent_id, order_num, url, menu_type, visible, is_refresh, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('任务新增', @taskMenuId, 2, '#', 'F', '0', '1', 'project:task:add', '#', 'admin', sysdate(), '', null, '');
INSERT INTO sys_menu (menu_name, parent_id, order_num, url, menu_type, visible, is_refresh, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('任务修改', @taskMenuId, 3, '#', 'F', '0', '1', 'project:task:edit', '#', 'admin', sysdate(), '', null, '');
INSERT INTO sys_menu (menu_name, parent_id, order_num, url, menu_type, visible, is_refresh, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('任务删除', @taskMenuId, 4, '#', 'F', '0', '1', 'project:task:remove', '#', 'admin', sysdate(), '', null, '');
INSERT INTO sys_menu (menu_name, parent_id, order_num, url, menu_type, visible, is_refresh, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('任务导出', @taskMenuId, 5, '#', 'F', '0', '1', 'project:task:export', '#', 'admin', sysdate(), '', null, '');

-- ----------------------------
-- 8. 示例数据（可选）
-- ----------------------------
INSERT INTO proj_project (project_name, project_status, manager, dept_id, priority, start_date, end_date, description, create_by, create_time, remark)
VALUES ('RuoYi二次开发项目', '1', 'admin', 103, '2', '2026-03-01', '2026-06-30', '基于RuoYi框架进行项目任务管理模块的二次开发', 'admin', sysdate(), '示例项目');

SET @demoProjectId = LAST_INSERT_ID();

INSERT INTO proj_task (task_name, project_id, task_status, priority, assignee, due_date, description, create_by, create_time, remark)
VALUES ('需求分析', @demoProjectId, '2', '2', 'admin', '2026-03-15', '完成项目需求文档分析和结构化JSON输出', 'admin', sysdate(), '');
INSERT INTO proj_task (task_name, project_id, task_status, priority, assignee, due_date, description, create_by, create_time, remark)
VALUES ('数据库设计', @demoProjectId, '1', '2', 'admin', '2026-03-20', '完成数据库表设计和SQL脚本编写', 'admin', sysdate(), '');
INSERT INTO proj_task (task_name, project_id, task_status, priority, assignee, due_date, description, create_by, create_time, remark)
VALUES ('后端开发', @demoProjectId, '0', '1', 'admin', '2026-04-15', '完成Domain/Mapper/Service/Controller开发', 'admin', sysdate(), '');
INSERT INTO proj_task (task_name, project_id, task_status, priority, assignee, due_date, description, create_by, create_time, remark)
VALUES ('前端页面', @demoProjectId, '0', '1', 'admin', '2026-04-30', '完成Thymeleaf模板页面开发', 'admin', sysdate(), '');
