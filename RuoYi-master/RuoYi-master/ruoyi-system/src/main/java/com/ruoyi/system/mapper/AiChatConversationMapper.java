package com.ruoyi.system.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
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
     * 软删除会话（仿FastGPT deleteTime机制）
     */
    public int softDeleteConversation(Long conversationId);

    /**
     * 切换置顶（仿FastGPT top字段）
     */
    public int toggleTopConversation(@Param("conversationId") Long conversationId, @Param("top") Integer top);

    /**
     * 清空用户所有对话历史（仿FastGPT clearHistories，软删除）
     */
    public int softDeleteAllByUserId(Long userId);

    /**
     * 物理删除会话
     */
    public int deleteAiChatConversationByConversationId(Long conversationId);

    /**
     * 批量物理删除会话
     */
    public int deleteAiChatConversationByConversationIds(String[] conversationIds);
}
