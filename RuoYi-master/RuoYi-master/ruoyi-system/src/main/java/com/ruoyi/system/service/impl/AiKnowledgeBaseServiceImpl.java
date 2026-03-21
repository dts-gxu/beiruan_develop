package com.ruoyi.system.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.system.domain.AiKnowledgeBase;
import com.ruoyi.system.mapper.AiKnowledgeBaseMapper;
import com.ruoyi.system.service.IAiKnowledgeBaseService;

/**
 * AI知识库Service实现
 * 
 * @author ruoyi
 */
@Service
public class AiKnowledgeBaseServiceImpl implements IAiKnowledgeBaseService
{
    @Autowired
    private AiKnowledgeBaseMapper aiKnowledgeBaseMapper;

    @Override
    public AiKnowledgeBase selectAiKnowledgeBaseByKbId(Long kbId)
    {
        return aiKnowledgeBaseMapper.selectAiKnowledgeBaseByKbId(kbId);
    }

    @Override
    public List<AiKnowledgeBase> selectAiKnowledgeBaseList(AiKnowledgeBase aiKnowledgeBase)
    {
        return aiKnowledgeBaseMapper.selectAiKnowledgeBaseList(aiKnowledgeBase);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertAiKnowledgeBase(AiKnowledgeBase aiKnowledgeBase)
    {
        return aiKnowledgeBaseMapper.insertAiKnowledgeBase(aiKnowledgeBase);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateAiKnowledgeBase(AiKnowledgeBase aiKnowledgeBase)
    {
        return aiKnowledgeBaseMapper.updateAiKnowledgeBase(aiKnowledgeBase);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteAiKnowledgeBaseByKbIds(Long[] kbIds)
    {
        return aiKnowledgeBaseMapper.deleteAiKnowledgeBaseByKbIds(kbIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteAiKnowledgeBaseByKbId(Long kbId)
    {
        return aiKnowledgeBaseMapper.deleteAiKnowledgeBaseByKbId(kbId);
    }
}
