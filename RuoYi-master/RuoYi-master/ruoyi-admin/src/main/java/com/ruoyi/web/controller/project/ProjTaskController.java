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
import com.ruoyi.system.domain.ProjTask;
import com.ruoyi.system.service.IProjProjectService;
import com.ruoyi.system.service.IProjTaskService;

/**
 * 项目任务 信息操作处理
 * 
 * @author ruoyi
 */
@Controller
@RequestMapping("/project/task")
public class ProjTaskController extends BaseController
{
    private String prefix = "project/task";

    @Autowired
    private IProjTaskService taskService;

    @Autowired
    private IProjProjectService projectService;

    @RequiresPermissions("project:task:view")
    @GetMapping()
    public String task()
    {
        return prefix + "/task";
    }

    /**
     * 查询任务列表
     */
    @RequiresPermissions("project:task:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(ProjTask task)
    {
        startPage();
        List<ProjTask> list = taskService.selectTaskList(task);
        return getDataTable(list);
    }

    /**
     * 导出任务列表
     */
    @Log(title = "任务管理", businessType = BusinessType.EXPORT)
    @RequiresPermissions("project:task:export")
    @PostMapping("/export")
    @ResponseBody
    public AjaxResult export(ProjTask task)
    {
        List<ProjTask> list = taskService.selectTaskList(task);
        ExcelUtil<ProjTask> util = new ExcelUtil<ProjTask>(ProjTask.class);
        return util.exportExcel(list, "任务数据");
    }

    /**
     * 新增任务
     */
    @RequiresPermissions("project:task:add")
    @GetMapping("/add")
    public String add(ModelMap mmap)
    {
        mmap.put("projects", projectService.selectProjectAll());
        return prefix + "/add";
    }

    /**
     * 新增保存任务
     */
    @RequiresPermissions("project:task:add")
    @Log(title = "任务管理", businessType = BusinessType.INSERT)
    @PostMapping("/add")
    @ResponseBody
    public AjaxResult addSave(@Validated ProjTask task)
    {
        task.setCreateBy(getLoginName());
        return toAjax(taskService.insertTask(task));
    }

    /**
     * 修改任务
     */
    @RequiresPermissions("project:task:edit")
    @GetMapping("/edit/{taskId}")
    public String edit(@PathVariable("taskId") Long taskId, ModelMap mmap)
    {
        mmap.put("task", taskService.selectTaskById(taskId));
        mmap.put("projects", projectService.selectProjectAll());
        return prefix + "/edit";
    }

    /**
     * 修改保存任务
     */
    @RequiresPermissions("project:task:edit")
    @Log(title = "任务管理", businessType = BusinessType.UPDATE)
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(@Validated ProjTask task)
    {
        task.setUpdateBy(getLoginName());
        return toAjax(taskService.updateTask(task));
    }

    /**
     * 删除任务
     */
    @RequiresPermissions("project:task:remove")
    @Log(title = "任务管理", businessType = BusinessType.DELETE)
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids)
    {
        return toAjax(taskService.deleteTaskByIds(ids));
    }
}
