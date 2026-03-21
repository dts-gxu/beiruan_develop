package com.ruoyi.system.mapper;

import java.util.List;
import com.ruoyi.system.domain.AiKnowledgeBase;

/**
 * AI知识库Mapper接口
 * 
 * @author ruoyi
 */
public interface AiKnowledgeBaseMapper
{
    public AiKnowledgeBase selectAiKnowledgeBaseByKbId(Long kbId);

    public List<AiKnowledgeBase> selectAiKnowledgeBaseList(AiKnowledgeBase aiKnowledgeBase);

    public int insertAiKnowledgeBase(AiKnowledgeBase aiKnowledgeBase);

    public int updateAiKnowledgeBase(AiKnowledgeBase aiKnowledgeBase);

    public int deleteAiKnowledgeBaseByKbId(Long kbId);

    public int deleteAiKnowledgeBaseByKbIds(Long[] kbIds);
}
