package com.ruoyi.system.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.system.domain.AiKnowledgeDocument;
import com.ruoyi.system.mapper.AiKnowledgeDocumentMapper;
import com.ruoyi.system.service.IAiKnowledgeDocumentService;

/**
 * AI知识库文档Service实现
 * 
 * @author ruoyi
 */
@Service
public class AiKnowledgeDocumentServiceImpl implements IAiKnowledgeDocumentService
{
    @Autowired
    private AiKnowledgeDocumentMapper aiKnowledgeDocumentMapper;

    @Override
    public AiKnowledgeDocument selectAiKnowledgeDocumentByDocId(Long docId)
    {
        return aiKnowledgeDocumentMapper.selectAiKnowledgeDocumentByDocId(docId);
    }

    @Override
    public List<AiKnowledgeDocument> selectAiKnowledgeDocumentList(AiKnowledgeDocument aiKnowledgeDocument)
    {
        return aiKnowledgeDocumentMapper.selectAiKnowledgeDocumentList(aiKnowledgeDocument);
    }

    @Override
    public List<AiKnowledgeDocument> selectAiKnowledgeDocumentListWithContent(Long kbId)
    {
        return aiKnowledgeDocumentMapper.selectAiKnowledgeDocumentListWithContent(kbId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertAiKnowledgeDocument(AiKnowledgeDocument aiKnowledgeDocument)
    {
        return aiKnowledgeDocumentMapper.insertAiKnowledgeDocument(aiKnowledgeDocument);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateAiKnowledgeDocument(AiKnowledgeDocument aiKnowledgeDocument)
    {
        return aiKnowledgeDocumentMapper.updateAiKnowledgeDocument(aiKnowledgeDocument);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteAiKnowledgeDocumentByDocIds(Long[] docIds)
    {
        return aiKnowledgeDocumentMapper.deleteAiKnowledgeDocumentByDocIds(docIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteAiKnowledgeDocumentByDocId(Long docId)
    {
        return aiKnowledgeDocumentMapper.deleteAiKnowledgeDocumentByDocId(docId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteAiKnowledgeDocumentByKbId(Long kbId)
    {
        return aiKnowledgeDocumentMapper.deleteAiKnowledgeDocumentByKbId(kbId);
    }

    @Override
    public int countByKbId(Long kbId)
    {
        return aiKnowledgeDocumentMapper.countByKbId(kbId);
    }

    @Override
    public AiKnowledgeDocument selectByDifyDocumentId(String difyDocumentId)
    {
        return aiKnowledgeDocumentMapper.selectByDifyDocumentId(difyDocumentId);
    }
}
