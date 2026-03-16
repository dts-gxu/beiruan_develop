package com.ruoyi.system.mapper;

import java.util.List;
import com.ruoyi.system.domain.ProjTask;

/**
 * 项目任务 数据层
 * 
 * @author ruoyi
 */
public interface ProjTaskMapper
{
    /**
     * 查询任务列表
     * 
     * @param task 任务信息
     * @return 任务集合
     */
    public List<ProjTask> selectTaskList(ProjTask task);

    /**
     * 通过任务ID查询任务信息
     * 
     * @param taskId 任务ID
     * @return 任务信息
     */
    public ProjTask selectTaskById(Long taskId);

    /**
     * 查询项目下的任务数量
     * 
     * @param projectId 项目ID
     * @return 任务数量
     */
    public int countTaskByProjectId(Long projectId);

    /**
     * 新增任务
     * 
     * @param task 任务信息
     * @return 结果
     */
    public int insertTask(ProjTask task);

    /**
     * 修改任务
     * 
     * @param task 任务信息
     * @return 结果
     */
    public int updateTask(ProjTask task);

    /**
     * 批量删除任务
     * 
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteTaskByIds(Long[] ids);
}
