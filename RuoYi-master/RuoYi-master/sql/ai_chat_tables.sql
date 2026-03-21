-- ----------------------------
-- AI对话会话表（如果已存在则跳过）
-- ----------------------------
CREATE TABLE IF NOT EXISTS ai_chat_conversation (
  conversation_id      BIGINT(20)      NOT NULL AUTO_INCREMENT    COMMENT '会话ID',
  app_id               BIGINT(20)      NOT NULL DEFAULT 0         COMMENT '关联应用ID（0=非Dify对话）',
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
-- AI对话消息表（如果已存在则跳过）
-- ----------------------------
CREATE TABLE IF NOT EXISTS ai_chat_message (
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
