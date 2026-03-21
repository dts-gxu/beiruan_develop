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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.AiDifyConfig;
import com.ruoyi.system.service.IAiDifyConfigService;
import com.ruoyi.system.service.impl.AiDifyConfigServiceImpl;

/**
 * Dify API连接配置Controller
 * 
 * @author ruoyi
 */
@Controller
@RequestMapping("/ai/dify/config")
public class AiDifyConfigController extends BaseController
{
    private String prefix = "ai/dify/config";

    @Autowired
    private IAiDifyConfigService aiDifyConfigService;

    @Autowired
    private DifyApiClient difyApiClient;

    @Autowired
    private DifyKnowledgeApiClient difyKnowledgeApiClient;

    @RequiresPermissions("ai:dify:config:view")
    @GetMapping()
    public String config()
    {
        return prefix + "/config";
    }

    /**
     * 查询配置列表
     */
    @RequiresPermissions("ai:dify:config:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(AiDifyConfig aiDifyConfig)
    {
        startPage();
        List<AiDifyConfig> list = aiDifyConfigService.selectAiDifyConfigList(aiDifyConfig);
        return getDataTable(list);
    }

    /**
     * 新增配置
     */
    @RequiresPermissions("ai:dify:config:add")
    @GetMapping("/add")
    public String add()
    {
        return prefix + "/add";
    }

    /**
     * 新增保存配置
     */
    @RequiresPermissions("ai:dify:config:add")
    @Log(title = "Dify配置管理", businessType = BusinessType.INSERT)
    @PostMapping("/add")
    @ResponseBody
    public AjaxResult addSave(@Validated AiDifyConfig aiDifyConfig)
    {
        aiDifyConfig.setCreateBy(getLoginName());
        return toAjax(aiDifyConfigService.insertAiDifyConfig(aiDifyConfig));
    }

    /**
     * 修改配置
     */
    @RequiresPermissions("ai:dify:config:edit")
    @GetMapping("/edit/{configId}")
    public String edit(@PathVariable("configId") Long configId, ModelMap mmap)
    {
        AiDifyConfig aiDifyConfig = aiDifyConfigService.selectAiDifyConfigByConfigId(configId);
        mmap.put("aiDifyConfig", aiDifyConfig);
        return prefix + "/edit";
    }

    /**
     * 修改保存配置
     */
    @RequiresPermissions("ai:dify:config:edit")
    @Log(title = "Dify配置管理", businessType = BusinessType.UPDATE)
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(@Validated AiDifyConfig aiDifyConfig)
    {
        aiDifyConfig.setUpdateBy(getLoginName());
        return toAjax(aiDifyConfigService.updateAiDifyConfig(aiDifyConfig));
    }

    /**
     * 删除配置
     */
    @RequiresPermissions("ai:dify:config:remove")
    @Log(title = "Dify配置管理", businessType = BusinessType.DELETE)
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids)
    {
        return toAjax(aiDifyConfigService.deleteAiDifyConfigByIds(ids));
    }

    /**
     * 测试连接
     */
    @RequiresPermissions("ai:dify:config:test")
    @PostMapping("/testConnection")
    @ResponseBody
    public AjaxResult testConnection(Long configId)
    {
        try
        {
            AiDifyConfig fullConfig = ((AiDifyConfigServiceImpl) aiDifyConfigService).selectRawConfigById(configId);
            if (fullConfig == null)
            {
                return AjaxResult.error("配置不存在");
            }

            String result;
            // Dataset类型用知识库API测试
            if ("3".equals(fullConfig.getConfigType()))
            {
                com.fasterxml.jackson.databind.JsonNode datasets = difyKnowledgeApiClient.listDatasets(
                    fullConfig.getBaseUrl(), fullConfig.getApiKey(), 1, 5);
                int total = datasets.path("total").asInt(0);
                result = "知识库API连接成功！共 " + total + " 个知识库";
            }
            else
            {
                result = "连接成功！应用名称：" + difyApiClient.getAppInfo(fullConfig.getApiKey(), fullConfig.getBaseUrl());
            }

            ((AiDifyConfigServiceImpl) aiDifyConfigService).updateTestResult(configId, true);
            return AjaxResult.success(result);
        }
        catch (Exception e)
        {
            ((AiDifyConfigServiceImpl) aiDifyConfigService).updateTestResult(configId, false);
            return AjaxResult.error("连接失败：" + e.getMessage());
        }
    }

    /**
     * 使用新输入的API Key和URL测试连接（新增页面用）
     */
    @RequiresPermissions("ai:dify:config:test")
    @PostMapping("/testNewConnection")
    @ResponseBody
    public AjaxResult testNewConnection(String baseUrl, String apiKey, @RequestParam(value = "configType", defaultValue = "0") String configType)
    {
        try
        {
            String result;
            if ("3".equals(configType))
            {
                com.fasterxml.jackson.databind.JsonNode datasets = difyKnowledgeApiClient.listDatasets(baseUrl, apiKey, 1, 5);
                int total = datasets.path("total").asInt(0);
                result = "知识库API连接成功！共 " + total + " 个知识库";
            }
            else
            {
                result = "连接成功！应用名称：" + difyApiClient.getAppInfo(apiKey, baseUrl);
            }
            return AjaxResult.success(result);
        }
        catch (Exception e)
        {
            return AjaxResult.error("连接失败：" + e.getMessage());
        }
    }

    /**
     * 获取Dataset类型的全局配置（供知识库模块使用）
     */
    @PostMapping("/getDatasetConfig")
    @ResponseBody
    public AjaxResult getDatasetConfig()
    {
        AiDifyConfig config = aiDifyConfigService.selectDefaultConfigByType("3");
        if (config == null)
        {
            return AjaxResult.error("未配置Dify知识库API，请先在Dify配置管理中添加Dataset类型配置");
        }
        AjaxResult result = AjaxResult.success();
        result.put("baseUrl", config.getBaseUrl());
        result.put("apiKey", config.getApiKey());
        result.put("configName", config.getConfigName());
        return result;
    }
}
