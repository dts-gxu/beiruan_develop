package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.AiChatConversation;
import com.ruoyi.system.domain.AiChatMessage;

/**
 * AI对话会话Service接口
 * 
 * @author ruoyi
 */
public interface IAiChatSessionService
{
    /**
     * 查询会话
     */
    public AiChatConversation selectConversationById(Long conversationId);

    /**
     * 查询用户的会话列表
     */
    public List<AiChatConversation> selectConversationList(AiChatConversation query);

    /**
     * 创建新会话
     */
    public AiChatConversation createConversation(Long appId, Long userId, String title, String conversationType);

    /**
     * 删除会话
     */
    public int deleteConversation(Long conversationId);

    /**
     * 批量删除会话
     */
    public int deleteConversationByIds(String ids);

    /**
     * 惰性创建会话（仿FastGPT的upsert模式）
     * 如果conversationId存在则返回该会话，否则自动创建新会话
     */
    public AiChatConversation ensureConversation(Long conversationId, Long userId, String firstMessage);

    /**
     * 发送消息
     */
    public AiChatMessage sendMessage(Long conversationId, String content);

    /**
     * 保存AI回复消息
     */
    public AiChatMessage saveAiReply(Long conversationId, String answer, String difyMessageId,
            String modelName, int tokensUsed, int costTime);

    /**
     * 更新会话的Dify会话ID
     */
    public void updateDifyConversationId(Long conversationId, String difyConversationId);

    /**
     * 查询会话的历史消息
     */
    public List<AiChatMessage> selectMessagesByConversationId(Long conversationId);

    /**
     * 修改会话标题
     */
    public int updateConversationTitle(Long conversationId, String title);

    /**
     * 切换置顶（仿FastGPT top字段）
     */
    public int toggleTopConversation(Long conversationId, Integer top);

    /**
     * 清空用户所有对话历史（仿FastGPT clearHistories，软删除）
     */
    public int clearAllConversations(Long userId);
}
