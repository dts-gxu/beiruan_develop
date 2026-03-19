package com.ruoyi.web.controller.ai;

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
import com.ruoyi.system.domain.AiDifyApp;
import com.ruoyi.system.domain.AiDifyConfig;
import com.ruoyi.system.service.IAiDifyAppService;
import com.ruoyi.system.service.IAiDifyConfigService;

/**
 * Dify应用管理Controller
 * 
 * @author ruoyi
 */
@Controller
@RequestMapping("/ai/dify/app")
public class AiDifyAppController extends BaseController
{
    private String prefix = "ai/dify/app";

    @Autowired
    private IAiDifyAppService aiDifyAppService;

    @Autowired
    private IAiDifyConfigService aiDifyConfigService;

    @Autowired
    private DifyApiClient difyApiClient;

    @RequiresPermissions("ai:dify:app:view")
    @GetMapping()
    public String app()
    {
        return prefix + "/app";
    }

    /**
     * 查询应用列表
     */
    @RequiresPermissions("ai:dify:app:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(AiDifyApp aiDifyApp)
    {
        startPage();
        List<AiDifyApp> list = aiDifyAppService.selectAiDifyAppList(aiDifyApp);
        return getDataTable(list);
    }

    /**
     * 查询可用应用列表（给chat.html下拉用）
     */
    @PostMapping("/activeList")
    @ResponseBody
    public AjaxResult activeList()
    {
        List<AiDifyApp> list = aiDifyAppService.selectActiveAppList();
        return AjaxResult.success(list);
    }

    /**
     * 新增应用
     */
    @RequiresPermissions("ai:dify:app:add")
    @GetMapping("/add")
    public String add(ModelMap mmap)
    {
        // 传递配置列表给前端下拉选择
        AiDifyConfig query = new AiDifyConfig();
        query.setStatus("0");
        List<AiDifyConfig> configs = aiDifyConfigService.selectAiDifyConfigList(query);
        mmap.put("configs", configs);
        return prefix + "/add";
    }

    /**
     * 新增保存应用
     */
    @RequiresPermissions("ai:dify:app:add")
    @Log(title = "Dify应用管理", businessType = BusinessType.INSERT)
    @PostMapping("/add")
    @ResponseBody
    public AjaxResult addSave(@Validated AiDifyApp aiDifyApp)
    {
        aiDifyApp.setCreateBy(getLoginName());
        return toAjax(aiDifyAppService.insertAiDifyApp(aiDifyApp));
    }

    /**
     * 修改应用
     */
    @RequiresPermissions("ai:dify:app:edit")
    @GetMapping("/edit/{appId}")
    public String edit(@PathVariable("appId") Long appId, ModelMap mmap)
    {
        AiDifyApp aiDifyApp = aiDifyAppService.selectAiDifyAppByAppId(appId);
        mmap.put("aiDifyApp", aiDifyApp);
        // 传递配置列表
        AiDifyConfig query = new AiDifyConfig();
        query.setStatus("0");
        List<AiDifyConfig> configs = aiDifyConfigService.selectAiDifyConfigList(query);
        mmap.put("configs", configs);
        return prefix + "/edit";
    }

    /**
     * 修改保存应用
     */
    @RequiresPermissions("ai:dify:app:edit")
    @Log(title = "Dify应用管理", businessType = BusinessType.UPDATE)
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(@Validated AiDifyApp aiDifyApp)
    {
        aiDifyApp.setUpdateBy(getLoginName());
        return toAjax(aiDifyAppService.updateAiDifyApp(aiDifyApp));
    }

    /**
     * 删除应用
     */
    @RequiresPermissions("ai:dify:app:remove")
    @Log(title = "Dify应用管理", businessType = BusinessType.DELETE)
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids)
    {
        return toAjax(aiDifyAppService.deleteAiDifyAppByIds(ids));
    }

    /**
     * 验证应用API密钥
     */
    @RequiresPermissions("ai:dify:app:test")
    @PostMapping("/testApiKey")
    @ResponseBody
    public AjaxResult testApiKey(Long appId)
    {
        try
        {
            AiDifyApp app = aiDifyAppService.selectAiDifyAppByAppId(appId);
            if (app == null)
            {
                return AjaxResult.error("应用不存在");
            }
            AiDifyConfig config = aiDifyConfigService.selectAiDifyConfigByConfigId(app.getConfigId());
            if (config == null)
            {
                return AjaxResult.error("关联配置不存在");
            }
            String params = difyApiClient.getAppParameters(app.getAppApiKey(), config.getBaseUrl());
            return AjaxResult.success("密钥验证成功", params);
        }
        catch (Exception e)
        {
            return AjaxResult.error("验证失败：" + e.getMessage());
        }
    }

    /**
     * 使用新输入的密钥验证（新增页面用）
     */
    @RequiresPermissions("ai:dify:app:test")
    @PostMapping("/testNewApiKey")
    @ResponseBody
    public AjaxResult testNewApiKey(Long configId, String appApiKey)
    {
        try
        {
            AiDifyConfig config = aiDifyConfigService.selectAiDifyConfigByConfigId(configId);
            if (config == null)
            {
                return AjaxResult.error("配置不存在");
            }
            String params = difyApiClient.getAppParameters(appApiKey, config.getBaseUrl());
            return AjaxResult.success("密钥验证成功", params);
        }
        catch (Exception e)
        {
            return AjaxResult.error("验证失败：" + e.getMessage());
        }
    }
}
