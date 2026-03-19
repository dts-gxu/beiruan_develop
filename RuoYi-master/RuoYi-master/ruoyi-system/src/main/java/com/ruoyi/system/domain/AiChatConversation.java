package com.ruoyi.system.domain;

import java.util.Date;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * AI对话会话对象 ai_chat_conversation
 * 
 * @author ruoyi
 */
public class AiChatConversation extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 会话ID */
    private Long conversationId;

    /** 关联应用ID */
    @Excel(name = "应用ID")
    private Long appId;

    /** 用户ID */
    @Excel(name = "用户ID")
    private Long userId;

    /** 会话标题 */
    @Excel(name = "会话标题")
    private String title;

    /** 会话类型（0=普通对话 1=需求分析 2=代码生成） */
    @Excel(name = "会话类型", readConverterExp = "0=普通对话,1=需求分析,2=代码生成")
    private String conversationType;

    /** Dify会话ID */
    private String difyConversationId;

    /** 消息数量 */
    @Excel(name = "消息数量")
    private Integer messageCount;

    /** 最后消息时间 */
    private Date lastMessageTime;

    /** 状态（0=正常 1=归档 2=删除） */
    @Excel(name = "状态", readConverterExp = "0=正常,1=归档,2=删除")
    private String status;

    /** 关联的应用名称（非数据库字段） */
    private String appName;

    /** 关联的用户名称（非数据库字段） */
    private String userName;

    public Long getConversationId()
    {
        return conversationId;
    }

    public void setConversationId(Long conversationId)
    {
        this.conversationId = conversationId;
    }

    public Long getAppId()
    {
        return appId;
    }

    public void setAppId(Long appId)
    {
        this.appId = appId;
    }

    public Long getUserId()
    {
        return userId;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public String getConversationType()
    {
        return conversationType;
    }

    public void setConversationType(String conversationType)
    {
        this.conversationType = conversationType;
    }

    public String getDifyConversationId()
    {
        return difyConversationId;
    }

    public void setDifyConversationId(String difyConversationId)
    {
        this.difyConversationId = difyConversationId;
    }

    public Integer getMessageCount()
    {
        return messageCount;
    }

    public void setMessageCount(Integer messageCount)
    {
        this.messageCount = messageCount;
    }

    public Date getLastMessageTime()
    {
        return lastMessageTime;
    }

    public void setLastMessageTime(Date lastMessageTime)
    {
        this.lastMessageTime = lastMessageTime;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public String getAppName()
    {
        return appName;
    }

    public void setAppName(String appName)
    {
        this.appName = appName;
    }

    public String getUserName()
    {
        return userName;
    }

    public void setUserName(String userName)
    {
        this.userName = userName;
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("conversationId", getConversationId())
            .append("appId", getAppId())
            .append("userId", getUserId())
            .append("title", getTitle())
            .append("conversationType", getConversationType())
            .append("difyConversationId", getDifyConversationId())
            .append("messageCount", getMessageCount())
            .append("lastMessageTime", getLastMessageTime())
            .append("status", getStatus())
            .append("createTime", getCreateTime())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
