package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.ProjProject;

/**
 * 项目管理 服务层
 * 
 * @author ruoyi
 */
public interface IProjProjectService
{
    /**
     * 查询项目列表
     * 
     * @param project 项目信息
     * @return 项目集合
     */
    public List<ProjProject> selectProjectList(ProjProject project);

    /**
     * 查询所有项目（用于下拉选择）
     * 
     * @return 项目列表
     */
    public List<ProjProject> selectProjectAll();

    /**
     * 通过项目ID查询项目信息
     * 
     * @param projectId 项目ID
     * @return 项目信息
     */
    public ProjProject selectProjectById(Long projectId);

    /**
     * 新增项目
     * 
     * @param project 项目信息
     * @return 结果
     */
    public int insertProject(ProjProject project);

    /**
     * 修改项目
     * 
     * @param project 项目信息
     * @return 结果
     */
    public int updateProject(ProjProject project);

    /**
     * 批量删除项目
     * 
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteProjectByIds(String ids);

    /**
     * 校验项目名称是否唯一
     * 
     * @param project 项目信息
     * @return 结果
     */
    public boolean checkProjectNameUnique(ProjProject project);
}
