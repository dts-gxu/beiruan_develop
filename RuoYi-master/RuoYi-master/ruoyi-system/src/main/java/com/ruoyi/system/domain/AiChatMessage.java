package com.ruoyi.system.domain;

import java.util.Date;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * AI对话消息对象 ai_chat_message
 * 
 * @author ruoyi
 */
public class AiChatMessage extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 消息ID */
    private Long messageId;

    /** 会话ID */
    @Excel(name = "会话ID")
    private Long conversationId;

    /** 角色（0=用户 1=AI助手 2=系统） */
    @Excel(name = "角色", readConverterExp = "0=用户,1=AI助手,2=系统")
    private String role;

    /** 消息内容 */
    @Excel(name = "消息内容")
    private String content;

    /** 内容类型（0=文本 1=代码 2=图片 3=文件） */
    @Excel(name = "内容类型", readConverterExp = "0=文本,1=代码,2=图片,3=文件")
    private String contentType;

    /** Dify消息ID */
    private String difyMessageId;

    /** 使用的模型名称 */
    private String modelName;

    /** Token使用量 */
    private Integer tokensUsed;

    /** 响应耗时（毫秒） */
    private Integer costTime;

    /** 状态（0=正常 1=已编辑 2=已删除） */
    @Excel(name = "状态", readConverterExp = "0=正常,1=已编辑,2=已删除")
    private String status;

    public Long getMessageId()
    {
        return messageId;
    }

    public void setMessageId(Long messageId)
    {
        this.messageId = messageId;
    }

    public Long getConversationId()
    {
        return conversationId;
    }

    public void setConversationId(Long conversationId)
    {
        this.conversationId = conversationId;
    }

    public String getRole()
    {
        return role;
    }

    public void setRole(String role)
    {
        this.role = role;
    }

    public String getContent()
    {
        return content;
    }

    public void setContent(String content)
    {
        this.content = content;
    }

    public String getContentType()
    {
        return contentType;
    }

    public void setContentType(String contentType)
    {
        this.contentType = contentType;
    }

    public String getDifyMessageId()
    {
        return difyMessageId;
    }

    public void setDifyMessageId(String difyMessageId)
    {
        this.difyMessageId = difyMessageId;
    }

    public String getModelName()
    {
        return modelName;
    }

    public void setModelName(String modelName)
    {
        this.modelName = modelName;
    }

    public Integer getTokensUsed()
    {
        return tokensUsed;
    }

    public void setTokensUsed(Integer tokensUsed)
    {
        this.tokensUsed = tokensUsed;
    }

    public Integer getCostTime()
    {
        return costTime;
    }

    public void setCostTime(Integer costTime)
    {
        this.costTime = costTime;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("messageId", getMessageId())
            .append("conversationId", getConversationId())
            .append("role", getRole())
            .append("content", getContent())
            .append("contentType", getContentType())
            .append("difyMessageId", getDifyMessageId())
            .append("modelName", getModelName())
            .append("tokensUsed", getTokensUsed())
            .append("costTime", getCostTime())
            .append("status", getStatus())
            .append("createTime", getCreateTime())
            .toString();
    }
}
