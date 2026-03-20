package com.ruoyi.system.service.impl;

import java.util.Date;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.core.text.Convert;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.system.domain.AiChatConversation;
import com.ruoyi.system.domain.AiChatMessage;
import com.ruoyi.system.mapper.AiChatConversationMapper;
import com.ruoyi.system.mapper.AiChatMessageMapper;
import com.ruoyi.system.service.IAiChatSessionService;

/**
 * AI对话会话Service业务层处理
 * 
 * @author ruoyi
 */
@Service
public class AiChatSessionServiceImpl implements IAiChatSessionService
{
    private static final Logger log = LoggerFactory.getLogger(AiChatSessionServiceImpl.class);

    @Autowired
    private AiChatConversationMapper conversationMapper;

    @Autowired
    private AiChatMessageMapper messageMapper;

    /**
     * 查询会话
     */
    @Override
    public AiChatConversation selectConversationById(Long conversationId)
    {
        return conversationMapper.selectAiChatConversationByConversationId(conversationId);
    }

    /**
     * 查询用户的会话列表
     */
    @Override
    public List<AiChatConversation> selectConversationList(AiChatConversation query)
    {
        return conversationMapper.selectAiChatConversationList(query);
    }

    /**
     * 创建新会话
     */
    @Override
    @Transactional
    public AiChatConversation createConversation(Long appId, Long userId, String title, String conversationType)
    {
        AiChatConversation conversation = new AiChatConversation();
        conversation.setAppId(appId);
        conversation.setUserId(userId);
        conversation.setTitle(title != null ? title : "新对话");
        conversation.setConversationType(conversationType != null ? conversationType : "0");
        conversation.setSource("online");
        conversation.setTop(0);
        conversation.setStatus("0");
        conversation.setMessageCount(0);
        conversation.setCreateTime(DateUtils.getNowDate());
        conversationMapper.insertAiChatConversation(conversation);
        log.info("创建AI会话: conversationId={}, appId={}, userId={}", conversation.getConversationId(), appId, userId);
        return conversation;
    }

    /**
     * 删除会话（仿FastGPT软删除机制，不物理删除数据）
     */
    @Override
    @Transactional
    public int deleteConversation(Long conversationId)
    {
        return conversationMapper.softDeleteConversation(conversationId);
    }

    /**
     * 批量删除会话
     */
    @Override
    @Transactional
    public int deleteConversationByIds(String ids)
    {
        String[] idArray = Convert.toStrArray(ids);
        for (String id : idArray)
        {
            messageMapper.deleteAiChatMessageByConversationId(Long.parseLong(id));
        }
        return conversationMapper.deleteAiChatConversationByConversationIds(idArray);
    }

    /**
     * 惰性创建会话（仿FastGPT的upsert模式）
     * 如果conversationId存在则返回该会话，否则自动创建新会话
     * 标题自动从第一条用户消息中提取前30字
     */
    @Override
    @Transactional
    public AiChatConversation ensureConversation(Long conversationId, Long userId, String firstMessage)
    {
        if (conversationId != null && conversationId > 0)
        {
            AiChatConversation existing = conversationMapper.selectAiChatConversationByConversationId(conversationId);
            if (existing != null)
            {
                return existing;
            }
        }
        // 自动创建：标题取自第一条消息的前30字
        String autoTitle = "新对话";
        if (firstMessage != null && !firstMessage.trim().isEmpty())
        {
            autoTitle = firstMessage.trim();
            if (autoTitle.length() > 30)
            {
                autoTitle = autoTitle.substring(0, 30) + "...";
            }
        }
        return createConversation(0L, userId, autoTitle, "0");
    }

    /**
     * 发送消息
     */
    @Override
    @Transactional
    public AiChatMessage sendMessage(Long conversationId, String content)
    {
        AiChatMessage userMessage = new AiChatMessage();
        userMessage.setConversationId(conversationId);
        userMessage.setRole("0");
        userMessage.setContent(content);
        userMessage.setContentType("0");
        userMessage.setStatus("0");
        userMessage.setCreateTime(DateUtils.getNowDate());
        messageMapper.insertAiChatMessage(userMessage);
        updateConversationStats(conversationId);
        return userMessage;
    }

    /**
     * 保存AI回复消息
     */
    @Override
    public AiChatMessage saveAiReply(Long conversationId, String answer, String difyMessageId,
            String modelName, int tokensUsed, int costTime)
    {
        AiChatMessage aiMessage = new AiChatMessage();
        aiMessage.setConversationId(conversationId);
        aiMessage.setRole("1");
        aiMessage.setContent(answer);
        aiMessage.setContentType("0");
        aiMessage.setDifyMessageId(difyMessageId);
        aiMessage.setModelName(modelName);
        aiMessage.setTokensUsed(tokensUsed);
        aiMessage.setCostTime(costTime);
        aiMessage.setStatus("0");
        aiMessage.setCreateTime(DateUtils.getNowDate());
        messageMapper.insertAiChatMessage(aiMessage);
        updateConversationStats(conversationId);
        return aiMessage;
    }

    /**
     * 更新会话的Dify会话ID
     */
    @Override
    public void updateDifyConversationId(Long conversationId, String difyConversationId)
    {
        AiChatConversation update = new AiChatConversation();
        update.setConversationId(conversationId);
        update.setDifyConversationId(difyConversationId);
        conversationMapper.updateAiChatConversation(update);
    }

    /**
     * 查询会话的历史消息
     */
    @Override
    public List<AiChatMessage> selectMessagesByConversationId(Long conversationId)
    {
        return messageMapper.selectMessagesByConversationId(conversationId);
    }

    /**
     * 修改会话标题
     */
    @Override
    public int updateConversationTitle(Long conversationId, String title)
    {
        AiChatConversation update = new AiChatConversation();
        update.setConversationId(conversationId);
        update.setTitle(title);
        update.setUpdateTime(DateUtils.getNowDate());
        return conversationMapper.updateAiChatConversation(update);
    }

    /**
     * 切换置顶（仿FastGPT top字段）
     */
    @Override
    public int toggleTopConversation(Long conversationId, Integer top)
    {
        return conversationMapper.toggleTopConversation(conversationId, top);
    }

    /**
     * 清空用户所有对话历史（仿FastGPT clearHistories，软删除）
     */
    @Override
    @Transactional
    public int clearAllConversations(Long userId)
    {
        return conversationMapper.softDeleteAllByUserId(userId);
    }

    /**
     * 更新会话统计
     */
    private void updateConversationStats(Long conversationId)
    {
        List<AiChatMessage> messages = messageMapper.selectMessagesByConversationId(conversationId);
        AiChatConversation update = new AiChatConversation();
        update.setConversationId(conversationId);
        update.setMessageCount(messages.size());
        update.setLastMessageTime(new Date());
        conversationMapper.updateAiChatConversation(update);
    }
}
