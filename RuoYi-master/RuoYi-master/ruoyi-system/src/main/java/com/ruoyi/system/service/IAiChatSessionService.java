package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.AiChatConversation;
import com.ruoyi.system.domain.AiChatMessage;

/**
 * AI对话会话Service接口
 * 
 * 核心职责：对话管理 + Dify API调用 + 消息持久化
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
     * 发送消息并获取AI回复（核心方法）
     * 
     * 流程：保存用户消息 → 调用Dify API → 保存AI回复 → 更新会话统计
     */
    public AiChatMessage sendMessage(Long conversationId, String content);

    /**
     * 查询会话的历史消息
     */
    public List<AiChatMessage> selectMessagesByConversationId(Long conversationId);

    /**
     * 修改会话标题
     */
    public int updateConversationTitle(Long conversationId, String title);
}
