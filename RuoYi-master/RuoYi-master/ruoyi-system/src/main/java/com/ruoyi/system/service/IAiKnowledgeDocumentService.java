package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.AiKnowledgeDocument;

/**
 * AI知识库文档Service接口
 * 
 * @author ruoyi
 */
public interface IAiKnowledgeDocumentService
{
    public AiKnowledgeDocument selectAiKnowledgeDocumentByDocId(Long docId);

    public List<AiKnowledgeDocument> selectAiKnowledgeDocumentList(AiKnowledgeDocument aiKnowledgeDocument);

    /**
     * 查询知识库文档列表（包含content字段，用于RAG上下文构建）
     */
    public List<AiKnowledgeDocument> selectAiKnowledgeDocumentListWithContent(Long kbId);

    public int insertAiKnowledgeDocument(AiKnowledgeDocument aiKnowledgeDocument);

    public int updateAiKnowledgeDocument(AiKnowledgeDocument aiKnowledgeDocument);

    public int deleteAiKnowledgeDocumentByDocIds(Long[] docIds);

    public int deleteAiKnowledgeDocumentByDocId(Long docId);

    public int deleteAiKnowledgeDocumentByKbId(Long kbId);

    public int countByKbId(Long kbId);

    /**
     * 通过Dify文档ID查询本地文档
     */
    public AiKnowledgeDocument selectByDifyDocumentId(String difyDocumentId);
}
