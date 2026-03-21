package com.ruoyi.system.mapper;

import java.util.List;
import com.ruoyi.system.domain.AiDifyConfig;

/**
 * Dify API连接配置Mapper接口
 * 
 * @author ruoyi
 */
public interface AiDifyConfigMapper
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
     * 新增配置
     */
    public int insertAiDifyConfig(AiDifyConfig aiDifyConfig);

    /**
     * 修改配置
     */
    public int updateAiDifyConfig(AiDifyConfig aiDifyConfig);

    /**
     * 删除配置
     */
    public int deleteAiDifyConfigByConfigId(Long configId);

    /**
     * 批量删除配置
     */
    public int deleteAiDifyConfigByConfigIds(String[] configIds);

    /**
     * 按类型查询默认配置
     */
    public AiDifyConfig selectDefaultConfigByType(String configType);

    /**
     * 重置所有默认标记
     */
    public int resetAllDefault();
}
