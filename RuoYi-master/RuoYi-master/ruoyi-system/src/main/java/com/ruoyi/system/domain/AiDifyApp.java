package com.ruoyi.system.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * Dify应用管理对象 ai_dify_app
 * 
 * @author ruoyi
 */
public class AiDifyApp extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 应用ID */
    private Long appId;

    /** 关联配置ID */
    @Excel(name = "配置ID")
    private Long configId;

    /** 应用名称 */
    @Excel(name = "应用名称")
    private String appName;

    /** 应用类型（0=Chat 1=Workflow 2=Completion 3=Agent） */
    @Excel(name = "应用类型", readConverterExp = "0=Chat,1=Workflow,2=Completion,3=Agent")
    private String appType;

    /** 应用API密钥 */
    private String appApiKey;

    /** 应用API密钥（脱敏显示） */
    private String appApiKeyMasked;

    /** 工作流ID（Workflow类型使用） */
    @Excel(name = "工作流ID")
    private String workflowId;

    /** 模型名称 */
    @Excel(name = "模型名称")
    private String modelName;

    /** 应用描述 */
    @Excel(name = "应用描述")
    private String description;

    /** 显示顺序 */
    private Integer sortOrder;

    /** 状态（0=正常 1=停用） */
    @Excel(name = "状态", readConverterExp = "0=正常,1=停用")
    private String status;

    /** 关联的配置名称（非数据库字段） */
    private String configName;

    public Long getAppId()
    {
        return appId;
    }

    public void setAppId(Long appId)
    {
        this.appId = appId;
    }

    public Long getConfigId()
    {
        return configId;
    }

    public void setConfigId(Long configId)
    {
        this.configId = configId;
    }

    public String getAppName()
    {
        return appName;
    }

    public void setAppName(String appName)
    {
        this.appName = appName;
    }

    public String getAppType()
    {
        return appType;
    }

    public void setAppType(String appType)
    {
        this.appType = appType;
    }

    public String getAppApiKey()
    {
        return appApiKey;
    }

    public void setAppApiKey(String appApiKey)
    {
        this.appApiKey = appApiKey;
    }

    public String getAppApiKeyMasked()
    {
        return appApiKeyMasked;
    }

    public void setAppApiKeyMasked(String appApiKeyMasked)
    {
        this.appApiKeyMasked = appApiKeyMasked;
    }

    public String getWorkflowId()
    {
        return workflowId;
    }

    public void setWorkflowId(String workflowId)
    {
        this.workflowId = workflowId;
    }

    public String getModelName()
    {
        return modelName;
    }

    public void setModelName(String modelName)
    {
        this.modelName = modelName;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public Integer getSortOrder()
    {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder)
    {
        this.sortOrder = sortOrder;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public String getConfigName()
    {
        return configName;
    }

    public void setConfigName(String configName)
    {
        this.configName = configName;
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("appId", getAppId())
            .append("configId", getConfigId())
            .append("appName", getAppName())
            .append("appType", getAppType())
            .append("workflowId", getWorkflowId())
            .append("modelName", getModelName())
            .append("description", getDescription())
            .append("sortOrder", getSortOrder())
            .append("status", getStatus())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
