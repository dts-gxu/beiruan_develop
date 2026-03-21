package com.ruoyi.system.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * AI知识库文档对象 ai_knowledge_document
 * 
 * @author ruoyi
 */
public class AiKnowledgeDocument extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 文档ID */
    private Long docId;

    /** 所属知识库ID */
    private Long kbId;

    /** 文档名称 */
    @Excel(name = "文档名称")
    private String docName;

    /** 文档类型（text/file/url） */
    @Excel(name = "文档类型")
    private String docType;

    /** 文档内容 */
    private String content;

    /** 文件路径 */
    private String filePath;

    /** 文件大小 */
    private Long fileSize;

    /** Dify文档ID */
    private String difyDocumentId;

    /** Dify批次号 */
    private String difyBatch;

    /** 索引状态 */
    @Excel(name = "索引状态")
    private String indexingStatus;

    /** 字数 */
    @Excel(name = "字数")
    private Integer wordCount;

    /** 命中次数 */
    private Integer hitCount;

    /** 是否启用 */
    private String enabled;

    /** 状态 */
    private String status;

    /** 删除标志 */
    private String delFlag;

    public Long getDocId()
    {
        return docId;
    }

    public void setDocId(Long docId)
    {
        this.docId = docId;
    }

    public Long getKbId()
    {
        return kbId;
    }

    public void setKbId(Long kbId)
    {
        this.kbId = kbId;
    }

    public String getDocName()
    {
        return docName;
    }

    public void setDocName(String docName)
    {
        this.docName = docName;
    }

    public String getDocType()
    {
        return docType;
    }

    public void setDocType(String docType)
    {
        this.docType = docType;
    }

    public String getContent()
    {
        return content;
    }

    public void setContent(String content)
    {
        this.content = content;
    }

    public String getFilePath()
    {
        return filePath;
    }

    public void setFilePath(String filePath)
    {
        this.filePath = filePath;
    }

    public Long getFileSize()
    {
        return fileSize;
    }

    public void setFileSize(Long fileSize)
    {
        this.fileSize = fileSize;
    }

    public String getDifyDocumentId()
    {
        return difyDocumentId;
    }

    public void setDifyDocumentId(String difyDocumentId)
    {
        this.difyDocumentId = difyDocumentId;
    }

    public String getDifyBatch()
    {
        return difyBatch;
    }

    public void setDifyBatch(String difyBatch)
    {
        this.difyBatch = difyBatch;
    }

    public String getIndexingStatus()
    {
        return indexingStatus;
    }

    public void setIndexingStatus(String indexingStatus)
    {
        this.indexingStatus = indexingStatus;
    }

    public Integer getWordCount()
    {
        return wordCount;
    }

    public void setWordCount(Integer wordCount)
    {
        this.wordCount = wordCount;
    }

    public Integer getHitCount()
    {
        return hitCount;
    }

    public void setHitCount(Integer hitCount)
    {
        this.hitCount = hitCount;
    }

    public String getEnabled()
    {
        return enabled;
    }

    public void setEnabled(String enabled)
    {
        this.enabled = enabled;
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
            .append("docId", getDocId())
            .append("kbId", getKbId())
            .append("docName", getDocName())
            .append("docType", getDocType())
            .append("difyDocumentId", getDifyDocumentId())
            .append("indexingStatus", getIndexingStatus())
            .append("wordCount", getWordCount())
            .append("createTime", getCreateTime())
            .toString();
    }
}
