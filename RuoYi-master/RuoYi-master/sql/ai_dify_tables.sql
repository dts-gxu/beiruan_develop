-- ----------------------------
-- Dify平台深度集成 数据库表
-- 版本：v2.0
-- 日期：2025-03-16
-- ----------------------------

-- ----------------------------
-- 1. Dify API连接配置表
-- ----------------------------
DROP TABLE IF EXISTS ai_dify_config;
CREATE TABLE ai_dify_config (
    config_id        BIGINT(20)    NOT NULL AUTO_INCREMENT  COMMENT '配置ID',
    config_name      VARCHAR(100)  NOT NULL                 COMMENT '配置名称',
    base_url         VARCHAR(500)  NOT NULL                 COMMENT 'Dify API地址',
    api_key          VARCHAR(500)  NOT NULL                 COMMENT 'API密钥（加密存储）',
    config_type      CHAR(1)       DEFAULT '0'              COMMENT '配置类型（0=Chat 1=Workflow 2=Completion）',
    is_default       CHAR(1)       DEFAULT 'N'              COMMENT '是否默认（Y=是 N=否）',
    status           CHAR(1)       DEFAULT '0'              COMMENT '状态（0=正常 1=停用）',
    last_test_time   DATETIME                               COMMENT '最后测试时间',
    last_test_result CHAR(1)                                COMMENT '最后测试结果（0=成功 1=失败）',
    del_flag         CHAR(1)       DEFAULT '0'              COMMENT '删除标志（0=存在 2=删除）',
    create_by        VARCHAR(64)   DEFAULT ''               COMMENT '创建者',
    create_time      DATETIME                               COMMENT '创建时间',
    update_by        VARCHAR(64)   DEFAULT ''               COMMENT '更新者',
    update_time      DATETIME                               COMMENT '更新时间',
    remark           VARCHAR(500)  DEFAULT NULL              COMMENT '备注',
    PRIMARY KEY (config_id)
) ENGINE=InnoDB AUTO_INCREMENT=1 COMMENT='Dify API连接配置表';

-- ----------------------------
-- 2. Dify应用管理表
-- ----------------------------
DROP TABLE IF EXISTS ai_dify_app;
CREATE TABLE ai_dify_app (
    app_id           BIGINT(20)    NOT NULL AUTO_INCREMENT  COMMENT '应用ID',
    config_id        BIGINT(20)    NOT NULL                 COMMENT '关联配置ID',
    app_name         VARCHAR(200)  NOT NULL                 COMMENT '应用名称',
    app_type         CHAR(1)       DEFAULT '0'              COMMENT '应用类型（0=Chat 1=Workflow 2=Completion 3=Agent）',
    app_api_key      VARCHAR(500)  NOT NULL                 COMMENT '应用API密钥',
    dify_app_id      VARCHAR(100)  DEFAULT ''               COMMENT 'Dify平台应用ID',
    workflow_id      VARCHAR(100)  DEFAULT ''               COMMENT '工作流ID',
    model_name       VARCHAR(100)  DEFAULT ''               COMMENT '模型名称',
    description      VARCHAR(1000) DEFAULT ''               COMMENT '应用描述',
    icon             VARCHAR(200)  DEFAULT ''               COMMENT '应用图标',
    sort_order       INT(4)        DEFAULT 0                COMMENT '排序号',
    status           CHAR(1)       DEFAULT '0'              COMMENT '状态（0=正常 1=停用）',
    del_flag         CHAR(1)       DEFAULT '0'              COMMENT '删除标志（0=存在 2=删除）',
    create_by        VARCHAR(64)   DEFAULT ''               COMMENT '创建者',
    create_time      DATETIME                               COMMENT '创建时间',
    update_by        VARCHAR(64)   DEFAULT ''               COMMENT '更新者',
    update_time      DATETIME                               COMMENT '更新时间',
    remark           VARCHAR(500)  DEFAULT NULL              COMMENT '备注',
    PRIMARY KEY (app_id),
    KEY idx_config_id (config_id)
) ENGINE=InnoDB AUTO_INCREMENT=1 COMMENT='Dify应用管理表';

-- ----------------------------
-- 3. AI对话会话表
-- ----------------------------
DROP TABLE IF EXISTS ai_chat_conversation;
CREATE TABLE ai_chat_conversation (
    conversation_id      BIGINT(20)    NOT NULL AUTO_INCREMENT  COMMENT '会话ID',
    app_id               BIGINT(20)    NOT NULL                 COMMENT '关联应用ID',
    user_id              BIGINT(20)    NOT NULL                 COMMENT '用户ID',
    dify_conversation_id VARCHAR(100)  DEFAULT ''               COMMENT 'Dify会话ID',
    title                VARCHAR(500)  DEFAULT '新对话'         COMMENT '会话标题',
    conversation_type    CHAR(1)       DEFAULT '0'              COMMENT '类型（0=普通 1=需求分析 2=代码生成 3=代码评审）',
    status               CHAR(1)       DEFAULT '0'              COMMENT '状态（0=进行中 1=已完成 2=已归档）',
    message_count        INT(8)        DEFAULT 0                COMMENT '消息数量',
    last_message_time    DATETIME                               COMMENT '最后消息时间',
    del_flag             CHAR(1)       DEFAULT '0'              COMMENT '删除标志（0=存在 2=删除）',
    create_by            VARCHAR(64)   DEFAULT ''               COMMENT '创建者',
    create_time          DATETIME                               COMMENT '创建时间',
    update_by            VARCHAR(64)   DEFAULT ''               COMMENT '更新者',
    update_time          DATETIME                               COMMENT '更新时间',
    remark               VARCHAR(500)  DEFAULT NULL              COMMENT '备注',
    PRIMARY KEY (conversation_id),
    KEY idx_app_id (app_id),
    KEY idx_user_id (user_id)
) ENGINE=InnoDB AUTO_INCREMENT=1 COMMENT='AI对话会话表';

-- ----------------------------
-- 4. AI对话消息表
-- ----------------------------
DROP TABLE IF EXISTS ai_chat_message;
CREATE TABLE ai_chat_message (
    message_id       BIGINT(20)    NOT NULL AUTO_INCREMENT  COMMENT '消息ID',
    conversation_id  BIGINT(20)    NOT NULL                 COMMENT '关联会话ID',
    role             CHAR(1)       DEFAULT '0'              COMMENT '角色（0=用户 1=AI 2=系统）',
    content          LONGTEXT                               COMMENT '消息内容',
    content_type     CHAR(1)       DEFAULT '0'              COMMENT '内容类型（0=文本 1=代码 2=图片 3=文件）',
    tokens_used      INT(8)        DEFAULT 0                COMMENT 'Token消耗',
    dify_message_id  VARCHAR(100)  DEFAULT ''               COMMENT 'Dify消息ID',
    model_name       VARCHAR(100)  DEFAULT ''               COMMENT '模型名称',
    cost_time        INT(8)        DEFAULT 0                COMMENT '响应耗时(ms)',
    status           CHAR(1)       DEFAULT '0'              COMMENT '状态（0=正常 1=失败）',
    create_time      DATETIME                               COMMENT '创建时间',
    PRIMARY KEY (message_id),
    KEY idx_conversation_id (conversation_id)
) ENGINE=InnoDB AUTO_INCREMENT=1 COMMENT='AI对话消息表';

-- ----------------------------
-- 字典类型
-- ----------------------------
INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark) VALUES
('Dify配置类型', 'ai_config_type', '0', 'admin', sysdate(), 'Dify API配置类型'),
('Dify应用类型', 'ai_app_type', '0', 'admin', sysdate(), 'Dify应用类型'),
('AI会话类型', 'ai_conversation_type', '0', 'admin', sysdate(), 'AI对话会话类型'),
('AI消息角色', 'ai_message_role', '0', 'admin', sysdate(), 'AI对话消息角色'),
('AI内容类型', 'ai_content_type', '0', 'admin', sysdate(), 'AI消息内容类型');

-- 配置类型
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time) VALUES
(1, 'Chat应用', '0', 'ai_config_type', '', 'primary', 'Y', '0', 'admin', sysdate()),
(2, 'Workflow应用', '1', 'ai_config_type', '', 'success', 'N', '0', 'admin', sysdate()),
(3, 'Completion应用', '2', 'ai_config_type', '', 'info', 'N', '0', 'admin', sysdate());

-- 应用类型
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time) VALUES
(1, 'Chat', '0', 'ai_app_type', '', 'primary', 'Y', '0', 'admin', sysdate()),
(2, 'Workflow', '1', 'ai_app_type', '', 'success', 'N', '0', 'admin', sysdate()),
(3, 'Completion', '2', 'ai_app_type', '', 'info', 'N', '0', 'admin', sysdate()),
(4, 'Agent', '3', 'ai_app_type', '', 'warning', 'N', '0', 'admin', sysdate());

-- 会话类型
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time) VALUES
(1, '普通对话', '0', 'ai_conversation_type', '', 'default', 'Y', '0', 'admin', sysdate()),
(2, '需求分析', '1', 'ai_conversation_type', '', 'primary', 'N', '0', 'admin', sysdate()),
(3, '代码生成', '2', 'ai_conversation_type', '', 'success', 'N', '0', 'admin', sysdate()),
(4, '代码评审', '3', 'ai_conversation_type', '', 'warning', 'N', '0', 'admin', sysdate());

-- 消息角色
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time) VALUES
(1, '用户', '0', 'ai_message_role', '', 'primary', 'Y', '0', 'admin', sysdate()),
(2, 'AI助手', '1', 'ai_message_role', '', 'success', 'N', '0', 'admin', sysdate()),
(3, '系统', '2', 'ai_message_role', '', 'info', 'N', '0', 'admin', sysdate());

-- 内容类型
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time) VALUES
(1, '文本', '0', 'ai_content_type', '', 'default', 'Y', '0', 'admin', sysdate()),
(2, '代码', '1', 'ai_content_type', '', 'primary', 'N', '0', 'admin', sysdate()),
(3, '图片', '2', 'ai_content_type', '', 'info', 'N', '0', 'admin', sysdate()),
(4, '文件', '3', 'ai_content_type', '', 'warning', 'N', '0', 'admin', sysdate());

-- ----------------------------
-- 菜单SQL
-- ----------------------------

-- 查找AI开发平台一级菜单ID（假设已存在，否则需先执行ai_menu.sql）
-- 这里使用变量方式，实际执行时需要根据实际menu_id替换
SET @aiPlatformId = (SELECT menu_id FROM sys_menu WHERE menu_name = 'AI开发平台' LIMIT 1);

-- Dify配置管理
INSERT INTO sys_menu (menu_name, parent_id, order_num, url, menu_type, visible, is_refresh, perms, icon, create_by, create_time)
VALUES ('Dify配置管理', @aiPlatformId, 2, '/ai/dify/config', 'C', '0', '1', 'ai:dify:config:view', 'fa fa-plug', 'admin', sysdate());
SET @configMenuId = LAST_INSERT_ID();

INSERT INTO sys_menu (menu_name, parent_id, order_num, url, menu_type, visible, is_refresh, perms, icon, create_by, create_time) VALUES
('配置查询', @configMenuId, 1, '#', 'F', '0', '1', 'ai:dify:config:list', '#', 'admin', sysdate()),
('配置新增', @configMenuId, 2, '#', 'F', '0', '1', 'ai:dify:config:add', '#', 'admin', sysdate()),
('配置修改', @configMenuId, 3, '#', 'F', '0', '1', 'ai:dify:config:edit', '#', 'admin', sysdate()),
('配置删除', @configMenuId, 4, '#', 'F', '0', '1', 'ai:dify:config:remove', '#', 'admin', sysdate()),
('连接测试', @configMenuId, 5, '#', 'F', '0', '1', 'ai:dify:config:test', '#', 'admin', sysdate());

-- Dify应用管理
INSERT INTO sys_menu (menu_name, parent_id, order_num, url, menu_type, visible, is_refresh, perms, icon, create_by, create_time)
VALUES ('Dify应用管理', @aiPlatformId, 3, '/ai/dify/app', 'C', '0', '1', 'ai:dify:app:view', 'fa fa-rocket', 'admin', sysdate());
SET @appMenuId = LAST_INSERT_ID();

INSERT INTO sys_menu (menu_name, parent_id, order_num, url, menu_type, visible, is_refresh, perms, icon, create_by, create_time) VALUES
('应用查询', @appMenuId, 1, '#', 'F', '0', '1', 'ai:dify:app:list', '#', 'admin', sysdate()),
('应用新增', @appMenuId, 2, '#', 'F', '0', '1', 'ai:dify:app:add', '#', 'admin', sysdate()),
('应用修改', @appMenuId, 3, '#', 'F', '0', '1', 'ai:dify:app:edit', '#', 'admin', sysdate()),
('应用删除', @appMenuId, 4, '#', 'F', '0', '1', 'ai:dify:app:remove', '#', 'admin', sysdate()),
('密钥验证', @appMenuId, 5, '#', 'F', '0', '1', 'ai:dify:app:test', '#', 'admin', sysdate());

-- 为admin角色分配菜单权限
INSERT INTO sys_role_menu (role_id, menu_id) VALUES (1, @configMenuId);
INSERT INTO sys_role_menu (role_id, menu_id) VALUES (1, @appMenuId);
INSERT INTO sys_role_menu (role_id, menu_id) SELECT 1, menu_id FROM sys_menu WHERE perms LIKE 'ai:dify:%';
