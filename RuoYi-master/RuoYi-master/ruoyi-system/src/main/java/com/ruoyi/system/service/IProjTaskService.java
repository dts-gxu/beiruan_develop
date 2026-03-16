package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.ProjTask;

/**
 * 项目任务 服务层
 * 
 * @author ruoyi
 */
public interface IProjTaskService
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
    public int deleteTaskByIds(String ids);
}
