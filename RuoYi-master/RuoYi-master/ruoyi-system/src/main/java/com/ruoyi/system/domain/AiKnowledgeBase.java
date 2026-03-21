package com.ruoyi.system.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * AI知识库对象 ai_knowledge_base
 * 
 * @author ruoyi
 */
public class AiKnowledgeBase extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 知识库ID */
    private Long kbId;

    /** 知识库名称 */
    @Excel(name = "知识库名称")
    private String kbName;

    /** 知识库描述 */
    @Excel(name = "知识库描述")
    private String kbDesc;

    /** 分类标签 */
    @Excel(name = "分类标签")
    private String category;

    /** Dify知识库ID */
    private String difyDatasetId;

    /** Dify Dataset API Key */
    private String difyApiKey;

    /** Dify API基础URL */
    private String difyBaseUrl;

    /** 文档数量 */
    @Excel(name = "文档数量")
    private Integer documentCount;

    /** 总字数 */
    @Excel(name = "总字数")
    private Integer wordCount;

    /** 状态（0正常 1停用） */
    @Excel(name = "状态", readConverterExp = "0=正常,1=停用")
    private String status;

    /** 删除标志（0存在 1删除） */
    private String delFlag;

    public Long getKbId()
    {
        return kbId;
    }

    public void setKbId(Long kbId)
    {
        this.kbId = kbId;
    }

    public String getKbName()
    {
        return kbName;
    }

    public void setKbName(String kbName)
    {
        this.kbName = kbName;
    }

    public String getKbDesc()
    {
        return kbDesc;
    }

    public void setKbDesc(String kbDesc)
    {
        this.kbDesc = kbDesc;
    }

    public String getCategory()
    {
        return category;
    }

    public void setCategory(String category)
    {
        this.category = category;
    }

    public String getDifyDatasetId()
    {
        return difyDatasetId;
    }

    public void setDifyDatasetId(String difyDatasetId)
    {
        this.difyDatasetId = difyDatasetId;
    }

    public String getDifyApiKey()
    {
        return difyApiKey;
    }

    public void setDifyApiKey(String difyApiKey)
    {
        this.difyApiKey = difyApiKey;
    }

    public String getDifyBaseUrl()
    {
        return difyBaseUrl;
    }

    public void setDifyBaseUrl(String difyBaseUrl)
    {
        this.difyBaseUrl = difyBaseUrl;
    }

    public Integer getDocumentCount()
    {
        return documentCount;
    }

    public void setDocumentCount(Integer documentCount)
    {
        this.documentCount = documentCount;
    }

    public Integer getWordCount()
    {
        return wordCount;
    }

    public void setWordCount(Integer wordCount)
    {
        this.wordCount = wordCount;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public String getDelFlag()
    {
        return delFlag;
    }

    public void setDelFlag(String delFlag)
    {
        this.delFlag = delFlag;
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("kbId", getKbId())
            .append("kbName", getKbName())
            .append("kbDesc", getKbDesc())
            .append("category", getCategory())
            .append("difyDatasetId", getDifyDatasetId())
            .append("documentCount", getDocumentCount())
            .append("wordCount", getWordCount())
            .append("status", getStatus())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .toString();
    }
}
