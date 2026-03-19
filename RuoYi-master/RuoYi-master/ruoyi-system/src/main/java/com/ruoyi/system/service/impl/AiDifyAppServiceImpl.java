package com.ruoyi.system.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.core.text.Convert;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.system.domain.AiDifyApp;
import com.ruoyi.system.mapper.AiDifyAppMapper;
import com.ruoyi.system.service.IAiDifyAppService;

/**
 * Dify应用管理Service业务层处理
 * 
 * @author ruoyi
 */
@Service
public class AiDifyAppServiceImpl implements IAiDifyAppService
{
    @Autowired
    private AiDifyAppMapper aiDifyAppMapper;

    /**
     * 查询应用
     */
    @Override
    public AiDifyApp selectAiDifyAppByAppId(Long appId)
    {
        AiDifyApp app = aiDifyAppMapper.selectAiDifyAppByAppId(appId);
        if (app != null)
        {
            app.setAppApiKeyMasked(maskApiKey(app.getAppApiKey()));
        }
        return app;
    }

    /**
     * 查询应用列表
     */
    @Override
    public List<AiDifyApp> selectAiDifyAppList(AiDifyApp aiDifyApp)
    {
        List<AiDifyApp> list = aiDifyAppMapper.selectAiDifyAppList(aiDifyApp);
        for (AiDifyApp app : list)
        {
            app.setAppApiKeyMasked(maskApiKey(app.getAppApiKey()));
            app.setAppApiKey(null);
        }
        return list;
    }

    /**
     * 查询可用应用列表
     */
    @Override
    public List<AiDifyApp> selectActiveAppList()
    {
        List<AiDifyApp> list = aiDifyAppMapper.selectActiveAppList();
        for (AiDifyApp app : list)
        {
            app.setAppApiKeyMasked(maskApiKey(app.getAppApiKey()));
        }
        return list;
    }

    /**
     * 新增应用
     */
    @Override
    public int insertAiDifyApp(AiDifyApp aiDifyApp)
    {
        aiDifyApp.setCreateTime(DateUtils.getNowDate());
        return aiDifyAppMapper.insertAiDifyApp(aiDifyApp);
    }

    /**
     * 修改应用
     */
    @Override
    public int updateAiDifyApp(AiDifyApp aiDifyApp)
    {
        aiDifyApp.setUpdateTime(DateUtils.getNowDate());
        return aiDifyAppMapper.updateAiDifyApp(aiDifyApp);
    }

    /**
     * 批量删除应用
     */
    @Override
    public int deleteAiDifyAppByIds(String ids)
    {
        return aiDifyAppMapper.deleteAiDifyAppByAppIds(Convert.toStrArray(ids));
    }

    /**
     * 删除应用
     */
    @Override
    public int deleteAiDifyAppByAppId(Long appId)
    {
        return aiDifyAppMapper.deleteAiDifyAppByAppId(appId);
    }

    /**
     * API Key脱敏
     */
    private String maskApiKey(String apiKey)
    {
        if (apiKey == null || apiKey.length() <= 8)
        {
            return "****";
        }
        return apiKey.substring(0, 4) + "****" + apiKey.substring(apiKey.length() - 4);
    }
}
