package com.ruoyi.system.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.text.Convert;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.ProjProject;
import com.ruoyi.system.mapper.ProjProjectMapper;
import com.ruoyi.system.mapper.ProjTaskMapper;
import com.ruoyi.system.service.IProjProjectService;

/**
 * 项目管理 服务层实现
 * 
 * @author ruoyi
 */
@Service
public class ProjProjectServiceImpl implements IProjProjectService
{
    @Autowired
    private ProjProjectMapper projectMapper;

    @Autowired
    private ProjTaskMapper taskMapper;

    /**
     * 查询项目列表
     * 
     * @param project 项目信息
     * @return 项目集合
     */
    @Override
    public List<ProjProject> selectProjectList(ProjProject project)
    {
        return projectMapper.selectProjectList(project);
    }

    /**
     * 查询所有项目（用于下拉选择）
     * 
     * @return 项目列表
     */
    @Override
    public List<ProjProject> selectProjectAll()
    {
        return projectMapper.selectProjectAll();
    }

    /**
     * 通过项目ID查询项目信息
     * 
     * @param projectId 项目ID
     * @return 项目信息
     */
    @Override
    public ProjProject selectProjectById(Long projectId)
    {
        return projectMapper.selectProjectById(projectId);
    }

    /**
     * 新增项目
     * 
     * @param project 项目信息
     * @return 结果
     */
    @Override
    public int insertProject(ProjProject project)
    {
        return projectMapper.insertProject(project);
    }

    /**
     * 修改项目
     * 
     * @param project 项目信息
     * @return 结果
     */
    @Override
    public int updateProject(ProjProject project)
    {
        return projectMapper.updateProject(project);
    }

    /**
     * 批量删除项目
     * 
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    @Override
    public int deleteProjectByIds(String ids)
    {
        Long[] projectIds = Convert.toLongArray(ids);
        for (Long projectId : projectIds)
        {
            int count = taskMapper.countTaskByProjectId(projectId);
            if (count > 0)
            {
                ProjProject project = selectProjectById(projectId);
                throw new ServiceException(String.format("项目【%1$s】下存在未完成的任务，不能删除", project.getProjectName()));
            }
        }
        return projectMapper.deleteProjectByIds(projectIds);
    }

    /**
     * 校验项目名称是否唯一
     * 
     * @param project 项目信息
     * @return 结果
     */
    @Override
    public boolean checkProjectNameUnique(ProjProject project)
    {
        Long projectId = StringUtils.isNull(project.getProjectId()) ? -1L : project.getProjectId();
        ProjProject info = projectMapper.checkProjectNameUnique(project.getProjectName());
        if (StringUtils.isNotNull(info) && info.getProjectId().longValue() != projectId.longValue())
        {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }
}
