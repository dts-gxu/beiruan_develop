package com.ruoyi.web.controller.project;

import java.util.List;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.system.domain.ProjProject;
import com.ruoyi.system.service.IProjProjectService;

/**
 * 项目管理 信息操作处理
 * 
 * @author ruoyi
 */
@Controller
@RequestMapping("/project/proj")
public class ProjProjectController extends BaseController
{
    private String prefix = "project/proj";

    @Autowired
    private IProjProjectService projectService;

    @RequiresPermissions("project:proj:view")
    @GetMapping()
    public String proj()
    {
        return prefix + "/proj";
    }

    /**
     * 查询项目列表
     */
    @RequiresPermissions("project:proj:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(ProjProject project)
    {
        startPage();
        List<ProjProject> list = projectService.selectProjectList(project);
        return getDataTable(list);
    }

    /**
     * 导出项目列表
     */
    @Log(title = "项目管理", businessType = BusinessType.EXPORT)
    @RequiresPermissions("project:proj:export")
    @PostMapping("/export")
    @ResponseBody
    public AjaxResult export(ProjProject project)
    {
        List<ProjProject> list = projectService.selectProjectList(project);
        ExcelUtil<ProjProject> util = new ExcelUtil<ProjProject>(ProjProject.class);
        return util.exportExcel(list, "项目数据");
    }

    /**
     * 新增项目
     */
    @RequiresPermissions("project:proj:add")
    @GetMapping("/add")
    public String add()
    {
        return prefix + "/add";
    }

    /**
     * 新增保存项目
     */
    @RequiresPermissions("project:proj:add")
    @Log(title = "项目管理", businessType = BusinessType.INSERT)
    @PostMapping("/add")
    @ResponseBody
    public AjaxResult addSave(@Validated ProjProject project)
    {
        if (!projectService.checkProjectNameUnique(project))
        {
            return error("新增项目'" + project.getProjectName() + "'失败，项目名称已存在");
        }
        project.setCreateBy(getLoginName());
        return toAjax(projectService.insertProject(project));
    }

    /**
     * 修改项目
     */
    @RequiresPermissions("project:proj:edit")
    @GetMapping("/edit/{projectId}")
    public String edit(@PathVariable("projectId") Long projectId, ModelMap mmap)
    {
        mmap.put("project", projectService.selectProjectById(projectId));
        return prefix + "/edit";
    }

    /**
     * 修改保存项目
     */
    @RequiresPermissions("project:proj:edit")
    @Log(title = "项目管理", businessType = BusinessType.UPDATE)
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(@Validated ProjProject project)
    {
        if (!projectService.checkProjectNameUnique(project))
        {
            return error("修改项目'" + project.getProjectName() + "'失败，项目名称已存在");
        }
        project.setUpdateBy(getLoginName());
        return toAjax(projectService.updateProject(project));
    }

    /**
     * 删除项目
     */
    @RequiresPermissions("project:proj:remove")
    @Log(title = "项目管理", businessType = BusinessType.DELETE)
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids)
    {
        return toAjax(projectService.deleteProjectByIds(ids));
    }

    /**
     * 校验项目名称
     */
    @PostMapping("/checkProjectNameUnique")
    @ResponseBody
    public boolean checkProjectNameUnique(ProjProject project)
    {
        return projectService.checkProjectNameUnique(project);
    }
}
