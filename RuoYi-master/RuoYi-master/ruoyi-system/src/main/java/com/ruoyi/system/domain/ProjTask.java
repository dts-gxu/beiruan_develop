package com.ruoyi.system.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.annotation.Excel.ColumnType;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 项目任务表 proj_task
 * 
 * @author ruoyi
 */
public class ProjTask extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 任务ID */
    @Excel(name = "任务ID", cellType = ColumnType.NUMERIC)
    private Long taskId;

    /** 任务名称 */
    @Excel(name = "任务名称")
    private String taskName;

    /** 所属项目ID */
    @Excel(name = "所属项目ID", cellType = ColumnType.NUMERIC)
    private Long projectId;

    /** 任务状态（0待处理 1进行中 2已完成 3已关闭） */
    @Excel(name = "任务状态", readConverterExp = "0=待处理,1=进行中,2=已完成,3=已关闭")
    private String taskStatus;

    /** 优先级（0低 1中 2高 3紧急） */
    @Excel(name = "优先级", readConverterExp = "0=低,1=中,2=高,3=紧急")
    private String priority;

    /** 负责人 */
    @Excel(name = "负责人")
    private String assignee;

    /** 截止日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "截止日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date dueDate;

    /** 实际完成日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "完成日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date completeDate;

    /** 任务描述 */
    private String description;

    /** 删除标志（0正常 2删除） */
    private String delFlag;

    /** 项目名称（非数据库字段，用于列表显示） */
    private String projectName;

    public Long getTaskId()
    {
        return taskId;
    }

    public void setTaskId(Long taskId)
    {
        this.taskId = taskId;
    }

    @NotBlank(message = "任务名称不能为空")
    @Size(min = 0, max = 200, message = "任务名称长度不能超过200个字符")
    public String getTaskName()
    {
        return taskName;
    }

    public void setTaskName(String taskName)
    {
        this.taskName = taskName;
    }

    @NotNull(message = "所属项目不能为空")
    public Long getProjectId()
    {
        return projectId;
    }

    public void setProjectId(Long projectId)
    {
        this.projectId = projectId;
    }

    public String getTaskStatus()
    {
        return taskStatus;
    }

    public void setTaskStatus(String taskStatus)
    {
        this.taskStatus = taskStatus;
    }

    public String getPriority()
    {
        return priority;
    }

    public void setPriority(String priority)
    {
        this.priority = priority;
    }

    @NotBlank(message = "负责人不能为空")
    public String getAssignee()
    {
        return assignee;
    }

    public void setAssignee(String assignee)
    {
        this.assignee = assignee;
    }

    public Date getDueDate()
    {
        return dueDate;
    }

    public void setDueDate(Date dueDate)
    {
        this.dueDate = dueDate;
    }

    public Date getCompleteDate()
    {
        return completeDate;
    }

    public void setCompleteDate(Date completeDate)
    {
        this.completeDate = completeDate;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public String getDelFlag()
    {
        return delFlag;
    }

    public void setDelFlag(String delFlag)
    {
        this.delFlag = delFlag;
    }

    public String getProjectName()
    {
        return projectName;
    }

    public void setProjectName(String projectName)
    {
        this.projectName = projectName;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("taskId", getTaskId())
            .append("taskName", getTaskName())
            .append("projectId", getProjectId())
            .append("taskStatus", getTaskStatus())
            .append("priority", getPriority())
            .append("assignee", getAssignee())
            .append("dueDate", getDueDate())
            .append("completeDate", getCompleteDate())
            .append("description", getDescription())
            .append("delFlag", getDelFlag())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
