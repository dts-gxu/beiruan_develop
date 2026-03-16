package com.ruoyi.system.mapper;

import java.util.List;
import com.ruoyi.system.domain.ProjProject;

/**
 * 项目管理 数据层
 * 
 * @author ruoyi
 */
public interface ProjProjectMapper
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
    public int deleteProjectByIds(Long[] ids);

    /**
     * 校验项目名称是否唯一
     * 
     * @param projectName 项目名称
     * @return 结果
     */
    public ProjProject checkProjectNameUnique(String projectName);
}
