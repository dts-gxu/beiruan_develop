package com.ruoyi.system.mapper;

import java.util.List;
import com.ruoyi.system.domain.AiDifyApp;

/**
 * Dify应用管理Mapper接口
 * 
 * @author ruoyi
 */
public interface AiDifyAppMapper
{
    /**
     * 查询应用
     */
    public AiDifyApp selectAiDifyAppByAppId(Long appId);

    /**
     * 查询应用列表（关联配置表）
     */
    public List<AiDifyApp> selectAiDifyAppList(AiDifyApp aiDifyApp);

    /**
     * 查询正常状态的应用列表
     */
    public List<AiDifyApp> selectActiveAppList();

    /**
     * 新增应用
     */
    public int insertAiDifyApp(AiDifyApp aiDifyApp);

    /**
     * 修改应用
     */
    public int updateAiDifyApp(AiDifyApp aiDifyApp);

    /**
     * 删除应用
     */
    public int deleteAiDifyAppByAppId(Long appId);

    /**
     * 批量删除应用
     */
    public int deleteAiDifyAppByAppIds(String[] appIds);
}
