package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.AiDifyApp;

/**
 * Dify应用管理Service接口
 * 
 * @author ruoyi
 */
public interface IAiDifyAppService
{
    /**
     * 查询应用
     */
    public AiDifyApp selectAiDifyAppByAppId(Long appId);

    /**
     * 查询应用列表
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
     * 批量删除应用
     */
    public int deleteAiDifyAppByIds(String ids);

    /**
     * 删除应用
     */
    public int deleteAiDifyAppByAppId(Long appId);

    /**
     * 验证应用API密钥
     */
    public String testAppApiKey(Long appId);
}
