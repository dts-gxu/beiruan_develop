package com.ruoyi.system.mapper;

import java.util.List;
import com.ruoyi.system.domain.AiChatMessage;

/**
 * AI对话消息Mapper接口
 * 
 * @author ruoyi
 */
public interface AiChatMessageMapper
{
    /**
     * 查询消息
     */
    public AiChatMessage selectAiChatMessageByMessageId(Long messageId);

    /**
     * 查询会话的消息列表
     */
    public List<AiChatMessage> selectAiChatMessageList(AiChatMessage aiChatMessage);

    /**
     * 查询会话的消息列表（按时间正序）
     */
    public List<AiChatMessage> selectMessagesByConversationId(Long conversationId);

    /**
     * 新增消息
     */
    public int insertAiChatMessage(AiChatMessage aiChatMessage);

    /**
     * 删除消息
     */
    public int deleteAiChatMessageByMessageId(Long messageId);

    /**
     * 根据会话ID删除消息
     */
    public int deleteAiChatMessageByConversationId(Long conversationId);
}
