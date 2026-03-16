package com.ruoyi.system.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.core.text.Convert;
import com.ruoyi.system.domain.ProjTask;
import com.ruoyi.system.mapper.ProjTaskMapper;
import com.ruoyi.system.service.IProjTaskService;

/**
 * 项目任务 服务层实现
 * 
 * @author ruoyi
 */
@Service
public class ProjTaskServiceImpl implements IProjTaskService
{
    @Autowired
    private ProjTaskMapper taskMapper;

    /**
     * 查询任务列表
     * 
     * @param task 任务信息
     * @return 任务集合
     */
    @Override
    public List<ProjTask> selectTaskList(ProjTask task)
    {
        return taskMapper.selectTaskList(task);
    }

    /**
     * 通过任务ID查询任务信息
     * 
     * @param taskId 任务ID
     * @return 任务信息
     */
    @Override
    public ProjTask selectTaskById(Long taskId)
    {
        return taskMapper.selectTaskById(taskId);
    }

    /**
     * 新增任务
     * 
     * @param task 任务信息
     * @return 结果
     */
    @Override
    public int insertTask(ProjTask task)
    {
        return taskMapper.insertTask(task);
    }

    /**
     * 修改任务
     * 
     * @param task 任务信息
     * @return 结果
     */
    @Override
    public int updateTask(ProjTask task)
    {
        return taskMapper.updateTask(task);
    }

    /**
     * 批量删除任务
     * 
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    @Override
    public int deleteTaskByIds(String ids)
    {
        Long[] taskIds = Convert.toLongArray(ids);
        return taskMapper.deleteTaskByIds(taskIds);
    }
}
