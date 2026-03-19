-- ----------------------------
-- Dify深度集成 - 数据库表结构
-- ----------------------------

-- ----------------------------
-- 1. Dify API连接配置表
-- ----------------------------
DROP TABLE IF EXISTS ai_dify_config;
CREATE TABLE ai_dify_config (
  config_id       BIGINT(20)      NOT NULL AUTO_INCREMENT    COMMENT '配置ID',
  config_name     VARCHAR(100)    NOT NULL                   COMMENT '配置名称',
  base_url        VARCHAR(255)    NOT NULL                   COMMENT 'Dify API基础URL',
  api_key         VARCHAR(255)    NOT NULL                   COMMENT 'API密钥',
  config_type     CHAR(1)         DEFAULT '0'                COMMENT '配置类型（0=Chat 1=Workflow 2=Completion）',
  is_default      CHAR(1)         DEFAULT 'N'                COMMENT '是否默认（Y=是 N=否）',
  status          CHAR(1)         DEFAULT '0'                COMMENT '状态（0=正常 1=停用）',
  last_test_time  DATETIME                                   COMMENT '最后测试时间',
  last_test_result CHAR(1)                                   COMMENT '最后测试结果（0=成功 1=失败）',
  create_by       VARCHAR(64)     DEFAULT ''                 COMMENT '创建者',
  create_time     DATETIME                                   COMMENT '创建时间',
  update_by       VARCHAR(64)     DEFAULT ''                 COMMENT '更新者',
  update_time     DATETIME                                   COMMENT '更新时间',
  remark          VARCHAR(500)    DEFAULT NULL               COMMENT '备注',
  PRIMARY KEY (config_id)
) ENGINE=InnoDB AUTO_INCREMENT=1 COMMENT='Dify API连接配置表';

-- ----------------------------
-- 2. Dify应用管理表
-- ----------------------------
DROP TABLE IF EXISTS ai_dify_app;
CREATE TABLE ai_dify_app (
  app_id          BIGINT(20)      NOT NULL AUTO_INCREMENT    COMMENT '应用ID',
  config_id       BIGINT(20)      NOT NULL                   COMMENT '关联配置ID',
  app_name        VARCHAR(100)    NOT NULL                   COMMENT '应用名称',
  app_type        CHAR(1)         DEFAULT '0'                COMMENT '应用类型（0=Chat 1=Workflow 2=Completion 3=Agent）',
  app_api_key     VARCHAR(255)    NOT NULL                   COMMENT '应用API密钥',
  workflow_id     VARCHAR(100)    DEFAULT NULL               COMMENT '工作流ID（Workflow类型使用）',
  model_name      VARCHAR(100)    DEFAULT NULL               COMMENT '模型名称',
  description     VARCHAR(500)    DEFAULT NULL               COMMENT '应用描述',
  sort_order      INT(4)          DEFAULT 0                  COMMENT '显示顺序',
  status          CHAR(1)         DEFAULT '0'                COMMENT '状态（0=正常 1=停用）',
  create_by       VARCHAR(64)     DEFAULT ''                 COMMENT '创建者',
  create_time     DATETIME                                   COMMENT '创建时间',
  update_by       VARCHAR(64)     DEFAULT ''                 COMMENT '更新者',
  update_time     DATETIME                                   COMMENT '更新时间',
  remark          VARCHAR(500)    DEFAULT NULL               COMMENT '备注',
  PRIMARY KEY (app_id),
  KEY idx_config_id (config_id)
) ENGINE=InnoDB AUTO_INCREMENT=1 COMMENT='Dify应用管理表';

-- ----------------------------
-- 3. AI对话会话表
-- ----------------------------
DROP TABLE IF EXISTS ai_chat_conversation;
CREATE TABLE ai_chat_conversation (
  conversation_id      BIGINT(20)      NOT NULL AUTO_INCREMENT    COMMENT '会话ID',
  app_id               BIGINT(20)      NOT NULL                   COMMENT '关联应用ID',
  user_id              BIGINT(20)      NOT NULL                   COMMENT '用户ID',
  title                VARCHAR(200)    DEFAULT '新对话'            COMMENT '会话标题',
  conversation_type    CHAR(1)         DEFAULT '0'                COMMENT '会话类型（0=普通对话 1=需求分析 2=代码生成）',
  dify_conversation_id VARCHAR(100)    DEFAULT NULL               COMMENT 'Dify会话ID',
  message_count        INT(11)         DEFAULT 0                  COMMENT '消息数量',
  last_message_time    DATETIME                                   COMMENT '最后消息时间',
  status               CHAR(1)         DEFAULT '0'                COMMENT '状态（0=正常 1=归档 2=删除）',
  create_time          DATETIME                                   COMMENT '创建时间',
  update_time          DATETIME                                   COMMENT '更新时间',
  PRIMARY KEY (conversation_id),
  KEY idx_app_id (app_id),
  KEY idx_user_id (user_id),
  KEY idx_create_time (create_time)
) ENGINE=InnoDB AUTO_INCREMENT=1 COMMENT='AI对话会话表';

-- ----------------------------
-- 4. AI对话消息表
-- ----------------------------
DROP TABLE IF EXISTS ai_chat_message;
CREATE TABLE ai_chat_message (
  message_id        BIGINT(20)      NOT NULL AUTO_INCREMENT    COMMENT '消息ID',
  conversation_id   BIGINT(20)      NOT NULL                   COMMENT '会话ID',
  role              CHAR(1)         NOT NULL                   COMMENT '角色（0=用户 1=AI助手 2=系统）',
  content           LONGTEXT                                   COMMENT '消息内容',
  content_type      CHAR(1)         DEFAULT '0'                COMMENT '内容类型（0=文本 1=代码 2=图片 3=文件）',
  dify_message_id   VARCHAR(100)    DEFAULT NULL               COMMENT 'Dify消息ID',
  model_name        VARCHAR(100)    DEFAULT NULL               COMMENT '使用的模型名称',
  tokens_used       INT(11)         DEFAULT 0                  COMMENT 'Token使用量',
  cost_time         INT(11)         DEFAULT 0                  COMMENT '响应耗时（毫秒）',
  status            CHAR(1)         DEFAULT '0'                COMMENT '状态（0=正常 1=已编辑 2=已删除）',
  create_time       DATETIME                                   COMMENT '创建时间',
  PRIMARY KEY (message_id),
  KEY idx_conversation_id (conversation_id),
  KEY idx_create_time (create_time)
) ENGINE=InnoDB AUTO_INCREMENT=1 COMMENT='AI对话消息表';

-- ----------------------------
-- 字典类型
-- ----------------------------
INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark) VALUES 
('Dify配置类型', 'ai_dify_config_type', '0', 'admin', sysdate(), 'Dify API配置类型'),
('Dify应用类型', 'ai_dify_app_type', '0', 'admin', sysdate(), 'Dify应用类型'),
('AI会话类型', 'ai_conversation_type', '0', 'admin', sysdate(), 'AI对话会话类型'),
('AI消息角色', 'ai_message_role', '0', 'admin', sysdate(), 'AI对话消息角色');

-- ----------------------------
-- 字典数据
-- ----------------------------
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark) VALUES 
(1, 'Chat', '0', 'ai_dify_config_type', '', 'primary', 'Y', '0', 'admin', sysdate(), 'Chat对话类型'),
(2, 'Workflow', '1', 'ai_dify_config_type', '', 'success', 'N', '0', 'admin', sysdate(), 'Workflow工作流类型'),
(3, 'Completion', '2', 'ai_dify_config_type', '', 'info', 'N', '0', 'admin', sysdate(), 'Completion补全类型');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark) VALUES 
(1, 'Chat', '0', 'ai_dify_app_type', '', 'primary', 'Y', '0', 'admin', sysdate(), 'Chat对话应用'),
(2, 'Workflow', '1', 'ai_dify_app_type', '', 'success', 'N', '0', 'admin', sysdate(), 'Workflow工作流应用'),
(3, 'Completion', '2', 'ai_dify_app_type', '', 'info', 'N', '0', 'admin', sysdate(), 'Completion补全应用'),
(4, 'Agent', '3', 'ai_dify_app_type', '', 'warning', 'N', '0', 'admin', sysdate(), 'Agent智能体应用');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark) VALUES 
(1, '普通对话', '0', 'ai_conversation_type', '', 'default', 'Y', '0', 'admin', sysdate(), '普通对话'),
(2, '需求分析', '1', 'ai_conversation_type', '', 'primary', 'N', '0', 'admin', sysdate(), '需求分析会话'),
(3, '代码生成', '2', 'ai_conversation_type', '', 'success', 'N', '0', 'admin', sysdate(), '代码生成会话');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark) VALUES 
(1, '用户', '0', 'ai_message_role', '', 'primary', 'Y', '0', 'admin', sysdate(), '用户消息'),
(2, 'AI助手', '1', 'ai_message_role', '', 'success', 'N', '0', 'admin', sysdate(), 'AI助手回复'),
(3, '系统', '2', 'ai_message_role', '', 'info', 'N', '0', 'admin', sysdate(), '系统消息');

-- ----------------------------
-- 菜单SQL（Dify配置管理 + Dify应用管理）
-- ----------------------------
-- 获取AI助手父菜单ID（假设已存在）
-- 如果不存在，需要先创建AI助手一级菜单

-- Dify配置管理菜单
INSERT INTO sys_menu (menu_name, parent_id, order_num, url, menu_type, visible, is_refresh, perms, icon, create_by, create_time, remark) 
SELECT 'Dify配置管理', menu_id, 10, '/ai/dify/config', 'C', '0', '1', 'ai:dify:config:view', 'fa fa-cogs', 'admin', sysdate(), 'Dify API配置管理菜单'
FROM sys_menu WHERE menu_name = 'AI助手' LIMIT 1;

SET @configMenuId = LAST_INSERT_ID();

INSERT INTO sys_menu (menu_name, parent_id, order_num, url, menu_type, visible, is_refresh, perms, icon, create_by, create_time, remark) VALUES 
('配置查询', @configMenuId, 1, '#', 'F', '0', '1', 'ai:dify:config:list', '#', 'admin', sysdate(), ''),
('配置新增', @configMenuId, 2, '#', 'F', '0', '1', 'ai:dify:config:add', '#', 'admin', sysdate(), ''),
('配置修改', @configMenuId, 3, '#', 'F', '0', '1', 'ai:dify:config:edit', '#', 'admin', sysdate(), ''),
('配置删除', @configMenuId, 4, '#', 'F', '0', '1', 'ai:dify:config:remove', '#', 'admin', sysdate(), ''),
('连接测试', @configMenuId, 5, '#', 'F', '0', '1', 'ai:dify:config:test', '#', 'admin', sysdate(), '');

-- Dify应用管理菜单
INSERT INTO sys_menu (menu_name, parent_id, order_num, url, menu_type, visible, is_refresh, perms, icon, create_by, create_time, remark) 
SELECT 'Dify应用管理', menu_id, 11, '/ai/dify/app', 'C', '0', '1', 'ai:dify:app:view', 'fa fa-cubes', 'admin', sysdate(), 'Dify应用管理菜单'
FROM sys_menu WHERE menu_name = 'AI助手' LIMIT 1;

SET @appMenuId = LAST_INSERT_ID();

INSERT INTO sys_menu (menu_name, parent_id, order_num, url, menu_type, visible, is_refresh, perms, icon, create_by, create_time, remark) VALUES 
('应用查询', @appMenuId, 1, '#', 'F', '0', '1', 'ai:dify:app:list', '#', 'admin', sysdate(), ''),
('应用新增', @appMenuId, 2, '#', 'F', '0', '1', 'ai:dify:app:add', '#', 'admin', sysdate(), ''),
('应用修改', @appMenuId, 3, '#', 'F', '0', '1', 'ai:dify:app:edit', '#', 'admin', sysdate(), ''),
('应用删除', @appMenuId, 4, '#', 'F', '0', '1', 'ai:dify:app:remove', '#', 'admin', sysdate(), ''),
('密钥验证', @appMenuId, 5, '#', 'F', '0', '1', 'ai:dify:app:test', '#', 'admin', sysdate(), '');
