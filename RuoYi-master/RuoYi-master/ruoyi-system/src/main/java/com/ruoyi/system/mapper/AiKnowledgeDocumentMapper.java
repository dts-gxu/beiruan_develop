package com.ruoyi.system.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.system.domain.AiKnowledgeDocument;

/**
 * AI知识库文档Mapper接口
 * 参考 max-serve AgentDatasetCollectionMapper 模式
 * 
 * @author ruoyi
 */
public interface AiKnowledgeDocumentMapper
{
    public AiKnowledgeDocument selectAiKnowledgeDocumentByDocId(Long docId);

    public List<AiKnowledgeDocument> selectAiKnowledgeDocumentList(AiKnowledgeDocument aiKnowledgeDocument);

    /**
     * 查询知识库文档列表（包含content字段，用于RAG上下文构建）
     * 注意：列表查询默认不含content以避免大文本影响性能
     */
    public List<AiKnowledgeDocument> selectAiKnowledgeDocumentListWithContent(@Param("kbId") Long kbId);

    public int insertAiKnowledgeDocument(AiKnowledgeDocument aiKnowledgeDocument);

    public int updateAiKnowledgeDocument(AiKnowledgeDocument aiKnowledgeDocument);

    public int deleteAiKnowledgeDocumentByDocId(Long docId);

    public int deleteAiKnowledgeDocumentByDocIds(Long[] docIds);

    public int deleteAiKnowledgeDocumentByKbId(Long kbId);

    public int countByKbId(Long kbId);

    public AiKnowledgeDocument selectByDifyDocumentId(@Param("difyDocumentId") String difyDocumentId);
}
