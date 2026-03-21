-- ============================================
-- AI知识库管理表
-- 参考 proj-hp-tdhpai/max-serve 的 agent_dataset 模式
-- 与Dify知识库API双向同步
-- ============================================

-- 1. 知识库主表
CREATE TABLE IF NOT EXISTS ai_knowledge_base (
  kb_id           BIGINT       NOT NULL AUTO_INCREMENT  COMMENT '知识库ID',
  kb_name         VARCHAR(200) NOT NULL                 COMMENT '知识库名称',
  kb_desc         VARCHAR(500) DEFAULT ''               COMMENT '知识库描述',
  category        VARCHAR(100) DEFAULT ''               COMMENT '分类标签',
  dify_dataset_id VARCHAR(100) DEFAULT ''               COMMENT 'Dify知识库ID（第三方同步）',
  dify_api_key    VARCHAR(500) DEFAULT ''               COMMENT 'Dify Dataset API Key',
  dify_base_url   VARCHAR(500) DEFAULT ''               COMMENT 'Dify API基础URL',
  document_count  INT          DEFAULT 0                COMMENT '文档数量',
  word_count      INT          DEFAULT 0                COMMENT '总字数',
  status          CHAR(1)      DEFAULT '0'              COMMENT '状态（0正常 1停用）',
  del_flag        CHAR(1)      DEFAULT '0'              COMMENT '删除标志（0存在 1删除）',
  create_by       VARCHAR(64)  DEFAULT ''               COMMENT '创建者',
  create_time     DATETIME                              COMMENT '创建时间',
  update_by       VARCHAR(64)  DEFAULT ''               COMMENT '更新者',
  update_time     DATETIME                              COMMENT '更新时间',
  remark          VARCHAR(500) DEFAULT ''               COMMENT '备注',
  PRIMARY KEY (kb_id),
  INDEX idx_dify_dataset_id (dify_dataset_id),
  INDEX idx_status (status)
) ENGINE=InnoDB AUTO_INCREMENT=1 COMMENT='AI知识库';

-- 2. 知识库文档表
CREATE TABLE IF NOT EXISTS ai_knowledge_document (
  doc_id            BIGINT        NOT NULL AUTO_INCREMENT  COMMENT '文档ID',
  kb_id             BIGINT        NOT NULL                 COMMENT '所属知识库ID',
  doc_name          VARCHAR(300)  NOT NULL                 COMMENT '文档名称',
  doc_type          VARCHAR(50)   DEFAULT 'text'           COMMENT '文档类型（text/file/url）',
  content           LONGTEXT                               COMMENT '文档内容（text类型时存储）',
  file_path         VARCHAR(500)  DEFAULT ''               COMMENT '文件路径（file类型时存储）',
  file_size         BIGINT        DEFAULT 0                COMMENT '文件大小（字节）',
  dify_document_id  VARCHAR(100)  DEFAULT ''               COMMENT 'Dify文档ID（第三方同步）',
  dify_batch        VARCHAR(100)  DEFAULT ''               COMMENT 'Dify批次号',
  indexing_status   VARCHAR(50)   DEFAULT 'waiting'        COMMENT '索引状态（waiting/indexing/completed/error）',
  word_count        INT           DEFAULT 0                COMMENT '字数',
  hit_count         INT           DEFAULT 0                COMMENT '命中次数',
  enabled           CHAR(1)       DEFAULT '1'              COMMENT '是否启用（0否 1是）',
  status            CHAR(1)       DEFAULT '0'              COMMENT '状态（0正常 1停用）',
  del_flag          CHAR(1)       DEFAULT '0'              COMMENT '删除标志（0存在 1删除）',
  create_by         VARCHAR(64)   DEFAULT ''               COMMENT '创建者',
  create_time       DATETIME                               COMMENT '创建时间',
  update_by         VARCHAR(64)   DEFAULT ''               COMMENT '更新者',
  update_time       DATETIME                               COMMENT '更新时间',
  remark            VARCHAR(500)  DEFAULT ''               COMMENT '备注',
  PRIMARY KEY (doc_id),
  INDEX idx_kb_id (kb_id),
  INDEX idx_dify_document_id (dify_document_id),
  INDEX idx_indexing_status (indexing_status)
) ENGINE=InnoDB AUTO_INCREMENT=1 COMMENT='AI知识库文档';
