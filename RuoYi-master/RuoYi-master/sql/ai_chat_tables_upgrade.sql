-- ============================================================
-- AI对话表结构升级脚本（仿FastGPT完整设计）
-- 在已有表基础上增加：置顶、自定义标题、软删除、用户反馈等字段
-- ============================================================

-- 1. 会话表增加字段
ALTER TABLE ai_chat_conversation 
  ADD COLUMN IF NOT EXISTS `custom_title` VARCHAR(200) DEFAULT NULL COMMENT '用户自定义标题（仿FastGPT customTitle）' AFTER `title`,
  ADD COLUMN IF NOT EXISTS `top` TINYINT(1) DEFAULT 0 COMMENT '是否置顶（仿FastGPT top字段）' AFTER `custom_title`,
  ADD COLUMN IF NOT EXISTS `source` VARCHAR(20) DEFAULT 'online' COMMENT '来源（online/api/share，仿FastGPT source字段）' AFTER `top`,
  ADD COLUMN IF NOT EXISTS `delete_time` DATETIME DEFAULT NULL COMMENT '软删除时间（仿FastGPT deleteTime，null=未删除）' AFTER `status`;

-- 2. 会话表增加索引（仿FastGPT的复合索引）
ALTER TABLE ai_chat_conversation
  ADD INDEX IF NOT EXISTS idx_user_app_update (user_id, app_id, last_message_time),
  ADD INDEX IF NOT EXISTS idx_user_top_update (user_id, top, last_message_time);

-- 3. 消息表增加字段
ALTER TABLE ai_chat_message
  ADD COLUMN IF NOT EXISTS `user_good_feedback` VARCHAR(500) DEFAULT NULL COMMENT '用户正面反馈（仿FastGPT）' AFTER `status`,
  ADD COLUMN IF NOT EXISTS `user_bad_feedback` VARCHAR(500) DEFAULT NULL COMMENT '用户负面反馈（仿FastGPT）' AFTER `user_good_feedback`,
  ADD COLUMN IF NOT EXISTS `duration_seconds` DECIMAL(10,2) DEFAULT NULL COMMENT '响应耗时（秒，仿FastGPT）' AFTER `user_bad_feedback`;

