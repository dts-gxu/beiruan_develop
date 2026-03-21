package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.AiKnowledgeBase;

/**
 * AI知识库Service接口
 * 
 * @author ruoyi
 */
public interface IAiKnowledgeBaseService
{
    public AiKnowledgeBase selectAiKnowledgeBaseByKbId(Long kbId);

    public List<AiKnowledgeBase> selectAiKnowledgeBaseList(AiKnowledgeBase aiKnowledgeBase);

    public int insertAiKnowledgeBase(AiKnowledgeBase aiKnowledgeBase);

    public int updateAiKnowledgeBase(AiKnowledgeBase aiKnowledgeBase);

    public int deleteAiKnowledgeBaseByKbIds(Long[] kbIds);

    public int deleteAiKnowledgeBaseByKbId(Long kbId);
}
