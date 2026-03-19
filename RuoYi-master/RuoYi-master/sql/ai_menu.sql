-- ----------------------------
-- AI开发助手 菜单SQL
-- ----------------------------

-- 一级菜单：AI开发平台
INSERT INTO sys_menu (menu_name, parent_id, order_num, url, menu_type, visible, is_refresh, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('AI开发平台', 0, 6, '#', 'M', '0', '1', '', 'fa fa-magic', 'admin', sysdate(), '', null, 'AI开发平台目录');

-- 获取刚插入的一级菜单ID
SET @parentId = LAST_INSERT_ID();

-- 二级菜单：需求分析
INSERT INTO sys_menu (menu_name, parent_id, order_num, url, menu_type, visible, is_refresh, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('AI需求分析', @parentId, 1, '/ai/assistant', 'C', '0', '1', 'ai:assistant:view', 'fa fa-file-text-o', 'admin', sysdate(), '', null, 'AI需求分析助手');

SET @menuId = LAST_INSERT_ID();

-- 按钮权限
INSERT INTO sys_menu (menu_name, parent_id, order_num, url, menu_type, visible, is_refresh, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('AI对话', @menuId, 1, '#', 'F', '0', '1', 'ai:assistant:chat', '#', 'admin', sysdate(), '', null, '');

INSERT INTO sys_menu (menu_name, parent_id, order_num, url, menu_type, visible, is_refresh, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('需求分析', @menuId, 2, '#', 'F', '0', '1', 'ai:assistant:analyze', '#', 'admin', sysdate(), '', null, '');

INSERT INTO sys_menu (menu_name, parent_id, order_num, url, menu_type, visible, is_refresh, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('代码生成', @menuId, 3, '#', 'F', '0', '1', 'ai:assistant:generate', '#', 'admin', sysdate(), '', null, '');

-- 为admin角色分配AI菜单权限（角色ID=1为超级管理员）
INSERT INTO sys_role_menu (role_id, menu_id) VALUES (1, @parentId);
INSERT INTO sys_role_menu (role_id, menu_id) VALUES (1, @menuId);
INSERT INTO sys_role_menu (role_id, menu_id) SELECT 1, menu_id FROM sys_menu WHERE perms IN ('ai:assistant:chat', 'ai:assistant:analyze', 'ai:assistant:generate');
