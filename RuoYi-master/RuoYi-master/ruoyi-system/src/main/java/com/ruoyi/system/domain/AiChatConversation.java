package com.ruoyi.system.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

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
    private Long appId;

    /** 用户ID */
    private Long userId;

    /** Dify会话ID */
    private String difyConversationId;

    /** 会话标题 */
    @Excel(name = "会话标题")
    private String title;

    /** 类型（0=普通 1=需求分析 2=代码生成 3=代码评审） */
    @Excel(name = "会话类型", readConverterExp = "0=普通,1=需求分析,2=代码生成,3=代码评审")
    private String conversationType;

    /** 状态（0=进行中 1=已完成 2=已归档） */
    @Excel(name = "状态", readConverterExp = "0=进行中,1=已完成,2=已归档")
    private String status;

    /** 消息数量 */
    private Integer messageCount;

    /** 最后消息时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastMessageTime;

    /** 删除标志（0=存在 2=删除） */
    private String delFlag;

    /** 关联应用名称（非数据库字段） */
    private String appName;

    /** 用户名称（非数据库字段） */
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

    public String getDifyConversationId()
    {
        return difyConversationId;
    }

    public void setDifyConversationId(String difyConversationId)
    {
        this.difyConversationId = difyConversationId;
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

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
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

    public String getDelFlag()
    {
        return delFlag;
    }

    public void setDelFlag(String delFlag)
    {
        this.delFlag = delFlag;
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
            .append("difyConversationId", getDifyConversationId())
            .append("title", getTitle())
            .append("conversationType", getConversationType())
            .append("status", getStatus())
            .append("messageCount", getMessageCount())
            .append("lastMessageTime", getLastMessageTime())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .toString();
    }
}
