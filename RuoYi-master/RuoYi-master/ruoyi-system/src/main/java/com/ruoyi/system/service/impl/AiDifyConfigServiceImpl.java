package com.ruoyi.system.service.impl;

import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.core.text.Convert;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.system.domain.AiDifyConfig;
import com.ruoyi.system.mapper.AiDifyConfigMapper;
import com.ruoyi.system.service.IAiDifyConfigService;

/**
 * Dify API连接配置Service业务层处理
 * 
 * @author ruoyi
 */
@Service
public class AiDifyConfigServiceImpl implements IAiDifyConfigService
{
    @Autowired
    private AiDifyConfigMapper aiDifyConfigMapper;

    /**
     * 查询配置
     */
    @Override
    public AiDifyConfig selectAiDifyConfigByConfigId(Long configId)
    {
        AiDifyConfig config = aiDifyConfigMapper.selectAiDifyConfigByConfigId(configId);
        if (config != null)
        {
            config.setApiKeyMasked(maskApiKey(config.getApiKey()));
        }
        return config;
    }

    /**
     * 查询配置列表
     */
    @Override
    public List<AiDifyConfig> selectAiDifyConfigList(AiDifyConfig aiDifyConfig)
    {
        List<AiDifyConfig> list = aiDifyConfigMapper.selectAiDifyConfigList(aiDifyConfig);
        for (AiDifyConfig config : list)
        {
            config.setApiKeyMasked(maskApiKey(config.getApiKey()));
            config.setApiKey(null);
        }
        return list;
    }

    /**
     * 查询默认配置
     */
    @Override
    public AiDifyConfig selectDefaultConfig()
    {
        return aiDifyConfigMapper.selectDefaultConfig();
    }

    /**
     * 新增配置
     */
    @Override
    @Transactional
    public int insertAiDifyConfig(AiDifyConfig aiDifyConfig)
    {
        aiDifyConfig.setCreateTime(DateUtils.getNowDate());
        if ("Y".equals(aiDifyConfig.getIsDefault()))
        {
            aiDifyConfigMapper.resetAllDefault();
        }
        return aiDifyConfigMapper.insertAiDifyConfig(aiDifyConfig);
    }

    /**
     * 修改配置
     */
    @Override
    @Transactional
    public int updateAiDifyConfig(AiDifyConfig aiDifyConfig)
    {
        aiDifyConfig.setUpdateTime(DateUtils.getNowDate());
        if ("Y".equals(aiDifyConfig.getIsDefault()))
        {
            aiDifyConfigMapper.resetAllDefault();
        }
        return aiDifyConfigMapper.updateAiDifyConfig(aiDifyConfig);
    }

    /**
     * 批量删除配置
     */
    @Override
    public int deleteAiDifyConfigByIds(String ids)
    {
        return aiDifyConfigMapper.deleteAiDifyConfigByConfigIds(Convert.toStrArray(ids));
    }

    /**
     * 删除配置
     */
    @Override
    public int deleteAiDifyConfigByConfigId(Long configId)
    {
        return aiDifyConfigMapper.deleteAiDifyConfigByConfigId(configId);
    }

    /**
     * 测试连接
     */
    @Override
    public String testConnection(Long configId)
    {
        return "请通过Controller调用测试";
    }

    /**
     * 查询配置（不脱敏，内部使用）
     */
    public AiDifyConfig selectRawConfigById(Long configId)
    {
        return aiDifyConfigMapper.selectAiDifyConfigByConfigId(configId);
    }

    /**
     * 更新测试结果
     */
    public void updateTestResult(Long configId, boolean success)
    {
        AiDifyConfig update = new AiDifyConfig();
        update.setConfigId(configId);
        update.setLastTestTime(new Date());
        update.setLastTestResult(success ? "0" : "1");
        aiDifyConfigMapper.updateAiDifyConfig(update);
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
