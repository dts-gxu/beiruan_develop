package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.AiDifyConfig;

/**
 * Dify API连接配置Service接口
 * 
 * @author ruoyi
 */
public interface IAiDifyConfigService
{
    /**
     * 查询配置
     */
    public AiDifyConfig selectAiDifyConfigByConfigId(Long configId);

    /**
     * 查询配置列表
     */
    public List<AiDifyConfig> selectAiDifyConfigList(AiDifyConfig aiDifyConfig);

    /**
     * 查询默认配置
     */
    public AiDifyConfig selectDefaultConfig();

    /**
     * 按类型查询默认配置（如 3=Dataset知识库）
     */
    public AiDifyConfig selectDefaultConfigByType(String configType);

    /**
     * 新增配置
     */
    public int insertAiDifyConfig(AiDifyConfig aiDifyConfig);

    /**
     * 修改配置
     */
    public int updateAiDifyConfig(AiDifyConfig aiDifyConfig);

    /**
     * 批量删除配置
     */
    public int deleteAiDifyConfigByIds(String ids);

    /**
     * 删除配置
     */
    public int deleteAiDifyConfigByConfigId(Long configId);

    /**
     * 测试连接
     */
    public String testConnection(Long configId);
}
