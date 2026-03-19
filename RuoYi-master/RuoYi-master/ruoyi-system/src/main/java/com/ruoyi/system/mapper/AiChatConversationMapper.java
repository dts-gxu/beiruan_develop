package com.ruoyi.system.mapper;

import java.util.List;
import com.ruoyi.system.domain.AiChatConversation;

/**
 * AI对话会话Mapper接口
 * 
 * @author ruoyi
 */
public interface AiChatConversationMapper
{
    /**
     * 查询会话
     */
    public AiChatConversation selectAiChatConversationByConversationId(Long conversationId);

    /**
     * 查询会话列表
     */
    public List<AiChatConversation> selectAiChatConversationList(AiChatConversation aiChatConversation);

    /**
     * 新增会话
     */
    public int insertAiChatConversation(AiChatConversation aiChatConversation);

    /**
     * 修改会话
     */
    public int updateAiChatConversation(AiChatConversation aiChatConversation);

    /**
     * 删除会话
     */
    public int deleteAiChatConversationByConversationId(Long conversationId);

    /**
     * 批量删除会话
     */
    public int deleteAiChatConversationByConversationIds(String[] conversationIds);
}
