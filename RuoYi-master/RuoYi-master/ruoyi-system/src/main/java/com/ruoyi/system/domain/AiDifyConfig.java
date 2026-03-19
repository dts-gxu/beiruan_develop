package com.ruoyi.system.domain;

import java.util.Date;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * Dify API连接配置对象 ai_dify_config
 * 
 * @author ruoyi
 */
public class AiDifyConfig extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 配置ID */
    private Long configId;

    /** 配置名称 */
    @Excel(name = "配置名称")
    private String configName;

    /** Dify API基础URL */
    @Excel(name = "API地址")
    private String baseUrl;

    /** API密钥 */
    private String apiKey;

    /** API密钥（脱敏显示） */
    private String apiKeyMasked;

    /** 配置类型（0=Chat 1=Workflow 2=Completion） */
    @Excel(name = "配置类型", readConverterExp = "0=Chat,1=Workflow,2=Completion")
    private String configType;

    /** 是否默认（Y=是 N=否） */
    @Excel(name = "是否默认", readConverterExp = "Y=是,N=否")
    private String isDefault;

    /** 状态（0=正常 1=停用） */
    @Excel(name = "状态", readConverterExp = "0=正常,1=停用")
    private String status;

    /** 最后测试时间 */
    private Date lastTestTime;

    /** 最后测试结果（0=成功 1=失败） */
    private String lastTestResult;

    public Long getConfigId()
    {
        return configId;
    }

    public void setConfigId(Long configId)
    {
        this.configId = configId;
    }

    public String getConfigName()
    {
        return configName;
    }

    public void setConfigName(String configName)
    {
        this.configName = configName;
    }

    public String getBaseUrl()
    {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl)
    {
        this.baseUrl = baseUrl;
    }

    public String getApiKey()
    {
        return apiKey;
    }

    public void setApiKey(String apiKey)
    {
        this.apiKey = apiKey;
    }

    public String getApiKeyMasked()
    {
        return apiKeyMasked;
    }

    public void setApiKeyMasked(String apiKeyMasked)
    {
        this.apiKeyMasked = apiKeyMasked;
    }

    public String getConfigType()
    {
        return configType;
    }

    public void setConfigType(String configType)
    {
        this.configType = configType;
    }

    public String getIsDefault()
    {
        return isDefault;
    }

    public void setIsDefault(String isDefault)
    {
        this.isDefault = isDefault;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public Date getLastTestTime()
    {
        return lastTestTime;
    }

    public void setLastTestTime(Date lastTestTime)
    {
        this.lastTestTime = lastTestTime;
    }

    public String getLastTestResult()
    {
        return lastTestResult;
    }

    public void setLastTestResult(String lastTestResult)
    {
        this.lastTestResult = lastTestResult;
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("configId", getConfigId())
            .append("configName", getConfigName())
            .append("baseUrl", getBaseUrl())
            .append("configType", getConfigType())
            .append("isDefault", getIsDefault())
            .append("status", getStatus())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
