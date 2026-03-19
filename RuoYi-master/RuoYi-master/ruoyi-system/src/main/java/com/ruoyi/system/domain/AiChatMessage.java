package com.ruoyi.system.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.annotation.Excel;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * AI对话消息对象 ai_chat_message
 * 
 * @author ruoyi
 */
public class AiChatMessage
{
    private static final long serialVersionUID = 1L;

    /** 消息ID */
    private Long messageId;

    /** 关联会话ID */
    private Long conversationId;

    /** 角色（0=用户 1=AI 2=系统） */
    @Excel(name = "角色", readConverterExp = "0=用户,1=AI,2=系统")
    private String role;

    /** 消息内容 */
    private String content;

    /** 内容类型（0=文本 1=代码 2=图片 3=文件） */
    private String contentType;

    /** Token消耗 */
    private Integer tokensUsed;

    /** Dify消息ID */
    private String difyMessageId;

    /** 模型名称 */
    private String modelName;

    /** 响应耗时(ms) */
    private Integer costTime;

    /** 状态（0=正常 1=失败） */
    private String status;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

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

    public Integer getTokensUsed()
    {
        return tokensUsed;
    }

    public void setTokensUsed(Integer tokensUsed)
    {
        this.tokensUsed = tokensUsed;
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

    public Date getCreateTime()
    {
        return createTime;
    }

    public void setCreateTime(Date createTime)
    {
        this.createTime = createTime;
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("messageId", getMessageId())
            .append("conversationId", getConversationId())
            .append("role", getRole())
            .append("contentType", getContentType())
            .append("tokensUsed", getTokensUsed())
            .append("modelName", getModelName())
            .append("costTime", getCostTime())
            .append("status", getStatus())
            .append("createTime", getCreateTime())
            .toString();
    }
}
