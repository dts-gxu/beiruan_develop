-- ============================================
-- AI知识库管理 - 菜单SQL
-- ============================================

-- 知识库管理菜单（挂在AI助手一级菜单下）
INSERT INTO sys_menu (menu_name, parent_id, order_num, url, menu_type, visible, is_refresh, perms, icon, create_by, create_time, remark) 
SELECT '知识库管理', menu_id, 12, '/ai/knowledge', 'C', '0', '1', 'ai:knowledge:view', 'fa fa-book', 'admin', sysdate(), ''
FROM sys_menu WHERE menu_name = 'AI开发平台' LIMIT 1;

SET @kbMenuId = LAST_INSERT_ID();

INSERT INTO sys_menu (menu_name, parent_id, order_num, url, menu_type, visible, is_refresh, perms, icon, create_by, create_time, remark) VALUES 
('知识库查询', @kbMenuId, 1, '#', 'F', '0', '1', 'ai:knowledge:list', '#', 'admin', sysdate(), ''),
('知识库新增', @kbMenuId, 2, '#', 'F', '0', '1', 'ai:knowledge:add', '#', 'admin', sysdate(), ''),
('知识库修改', @kbMenuId, 3, '#', 'F', '0', '1', 'ai:knowledge:edit', '#', 'admin', sysdate(), ''),
('知识库删除', @kbMenuId, 4, '#', 'F', '0', '1', 'ai:knowledge:remove', '#', 'admin', sysdate(), '');
