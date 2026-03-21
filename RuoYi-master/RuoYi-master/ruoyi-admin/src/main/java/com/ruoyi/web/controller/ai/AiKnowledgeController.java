package com.ruoyi.web.controller.ai;

import java.util.List;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import com.fasterxml.jackson.databind.JsonNode;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.ShiroUtils;
import com.ruoyi.system.domain.AiKnowledgeBase;
import com.ruoyi.system.domain.AiKnowledgeDocument;
import com.ruoyi.system.service.IAiKnowledgeBaseService;
import com.ruoyi.system.service.IAiKnowledgeDocumentService;

/**
 * AI知识库管理Controller
 * 参考 proj-hp-tdhpai/max-serve 的 AgentDatasetController 模式
 * RuoYi本地CRUD + Dify知识库API双向同步
 * 
 * @author ruoyi
 */
@Controller
@RequestMapping("/ai/knowledge")
public class AiKnowledgeController extends BaseController
{
    private static final Logger log = LoggerFactory.getLogger(AiKnowledgeController.class);

    private String prefix = "ai/knowledge";

    @Autowired
    private IAiKnowledgeBaseService knowledgeBaseService;

    @Autowired
    private IAiKnowledgeDocumentService knowledgeDocumentService;

    @Autowired
    private DifyKnowledgeApiClient difyKnowledgeApi;

    // ==================== 页面路由 ====================

    @RequiresPermissions("ai:knowledge:view")
    @GetMapping()
    public String knowledge()
    {
        return prefix + "/list";
    }

    @RequiresPermissions("ai:knowledge:add")
    @GetMapping("/add")
    public String add()
    {
        return prefix + "/add";
    }

    @RequiresPermissions("ai:knowledge:edit")
    @GetMapping("/edit/{kbId}")
    public String edit(@PathVariable("kbId") Long kbId, ModelMap mmap)
    {
        AiKnowledgeBase kb = knowledgeBaseService.selectAiKnowledgeBaseByKbId(kbId);
        mmap.put("kb", kb);
        return prefix + "/edit";
    }

    @RequiresPermissions("ai:knowledge:view")
    @GetMapping("/documents/{kbId}")
    public String documents(@PathVariable("kbId") Long kbId, ModelMap mmap)
    {
        AiKnowledgeBase kb = knowledgeBaseService.selectAiKnowledgeBaseByKbId(kbId);
        mmap.put("kb", kb);
        return prefix + "/documents";
    }

    @RequiresPermissions("ai:knowledge:add")
    @GetMapping("/addDocument/{kbId}")
    public String addDocument(@PathVariable("kbId") Long kbId, ModelMap mmap)
    {
        mmap.put("kbId", kbId);
        return prefix + "/addDocument";
    }

    @RequiresPermissions("ai:knowledge:edit")
    @GetMapping("/editDocument/{docId}")
    public String editDocument(@PathVariable("docId") Long docId, ModelMap mmap)
    {
        AiKnowledgeDocument doc = knowledgeDocumentService.selectAiKnowledgeDocumentByDocId(docId);
        mmap.put("doc", doc);
        return prefix + "/editDocument";
    }

    // ==================== 知识库CRUD ====================

    @RequiresPermissions("ai:knowledge:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(AiKnowledgeBase aiKnowledgeBase)
    {
        startPage();
        List<AiKnowledgeBase> list = knowledgeBaseService.selectAiKnowledgeBaseList(aiKnowledgeBase);
        return getDataTable(list);
    }

    @RequiresPermissions("ai:knowledge:add")
    @Log(title = "知识库管理", businessType = BusinessType.INSERT)
    @PostMapping("/add")
    @ResponseBody
    public AjaxResult addSave(AiKnowledgeBase aiKnowledgeBase)
    {
        aiKnowledgeBase.setCreateBy(ShiroUtils.getLoginName());
        // 先保存到本地数据库
        int rows = knowledgeBaseService.insertAiKnowledgeBase(aiKnowledgeBase);

        // 同步到Dify（如果配置了API Key）
        if (rows > 0 && isNotEmpty(aiKnowledgeBase.getDifyApiKey()) && isNotEmpty(aiKnowledgeBase.getDifyBaseUrl()))
        {
            try
            {
                JsonNode result = difyKnowledgeApi.createDataset(
                    aiKnowledgeBase.getDifyBaseUrl(),
                    aiKnowledgeBase.getDifyApiKey(),
                    aiKnowledgeBase.getKbName(),
                    aiKnowledgeBase.getKbDesc()
                );
                String difyDatasetId = result.path("id").asText("");
                if (!difyDatasetId.isEmpty())
                {
                    aiKnowledgeBase.setDifyDatasetId(difyDatasetId);
                    knowledgeBaseService.updateAiKnowledgeBase(aiKnowledgeBase);
                    log.info("知识库同步到Dify成功: kbId={}, difyDatasetId={}", aiKnowledgeBase.getKbId(), difyDatasetId);
                }
            }
            catch (Exception e)
            {
                log.error("知识库同步到Dify失败: kbName={}", aiKnowledgeBase.getKbName(), e);
                // 本地已保存成功，不影响
            }
        }
        return toAjax(rows);
    }

    @RequiresPermissions("ai:knowledge:edit")
    @Log(title = "知识库管理", businessType = BusinessType.UPDATE)
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(AiKnowledgeBase aiKnowledgeBase)
    {
        aiKnowledgeBase.setUpdateBy(ShiroUtils.getLoginName());

        // 检查是否首次配置Dify（之前没有difyDatasetId，现在有了apiKey和baseUrl）
        AiKnowledgeBase existing = knowledgeBaseService.selectAiKnowledgeBaseByKbId(aiKnowledgeBase.getKbId());
        boolean needCreateDify = existing != null
            && !isNotEmpty(existing.getDifyDatasetId())
            && isNotEmpty(aiKnowledgeBase.getDifyApiKey())
            && isNotEmpty(aiKnowledgeBase.getDifyBaseUrl());

        int rows = knowledgeBaseService.updateAiKnowledgeBase(aiKnowledgeBase);

        // 首次配置Dify时自动创建知识库
        if (rows > 0 && needCreateDify)
        {
            try
            {
                String kbName = isNotEmpty(aiKnowledgeBase.getKbName()) ? aiKnowledgeBase.getKbName() : existing.getKbName();
                String kbDesc = aiKnowledgeBase.getKbDesc() != null ? aiKnowledgeBase.getKbDesc() : existing.getKbDesc();
                JsonNode result = difyKnowledgeApi.createDataset(
                    aiKnowledgeBase.getDifyBaseUrl(),
                    aiKnowledgeBase.getDifyApiKey(),
                    kbName, kbDesc
                );
                String difyDatasetId = result.path("id").asText("");
                if (!difyDatasetId.isEmpty())
                {
                    AiKnowledgeBase update = new AiKnowledgeBase();
                    update.setKbId(aiKnowledgeBase.getKbId());
                    update.setDifyDatasetId(difyDatasetId);
                    knowledgeBaseService.updateAiKnowledgeBase(update);
                    log.info("编辑知识库时首次同步到Dify成功: kbId={}, difyDatasetId={}", aiKnowledgeBase.getKbId(), difyDatasetId);
                }
            }
            catch (Exception e)
            {
                log.error("编辑知识库时同步到Dify失败: kbId={}", aiKnowledgeBase.getKbId(), e);
            }
        }
        return toAjax(rows);
    }

    @RequiresPermissions("ai:knowledge:remove")
    @Log(title = "知识库管理", businessType = BusinessType.DELETE)
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids)
    {
        Long[] kbIds = convertStrToLongArray(ids);
        // 删除Dify端知识库
        for (Long kbId : kbIds)
        {
            AiKnowledgeBase kb = knowledgeBaseService.selectAiKnowledgeBaseByKbId(kbId);
            if (kb != null && isNotEmpty(kb.getDifyDatasetId()) && isNotEmpty(kb.getDifyApiKey()))
            {
                try
                {
                    difyKnowledgeApi.deleteDataset(kb.getDifyBaseUrl(), kb.getDifyApiKey(), kb.getDifyDatasetId());
                    log.info("Dify知识库删除成功: difyDatasetId={}", kb.getDifyDatasetId());
                }
                catch (Exception e)
                {
                    log.warn("Dify知识库删除失败（忽略）: difyDatasetId={}", kb.getDifyDatasetId(), e);
                }
            }
            // 同时删除文档记录
            knowledgeDocumentService.deleteAiKnowledgeDocumentByKbId(kbId);
        }
        return toAjax(knowledgeBaseService.deleteAiKnowledgeBaseByKbIds(kbIds));
    }

    // ==================== 文档管理 ====================

    @RequiresPermissions("ai:knowledge:list")
    @PostMapping("/document/list")
    @ResponseBody
    public TableDataInfo documentList(AiKnowledgeDocument doc)
    {
        startPage();
        List<AiKnowledgeDocument> list = knowledgeDocumentService.selectAiKnowledgeDocumentList(doc);
        return getDataTable(list);
    }

    /**
     * 添加文本文档（同步到Dify）
     */
    @RequiresPermissions("ai:knowledge:add")
    @Log(title = "知识库文档", businessType = BusinessType.INSERT)
    @PostMapping("/document/addText")
    @ResponseBody
    public AjaxResult addTextDocument(@RequestParam("kbId") Long kbId,
                                       @RequestParam("docName") String docName,
                                       @RequestParam("content") String content)
    {
        AiKnowledgeBase kb = knowledgeBaseService.selectAiKnowledgeBaseByKbId(kbId);
        if (kb == null)
        {
            return error("知识库不存在");
        }

        // 创建本地文档记录
        AiKnowledgeDocument doc = new AiKnowledgeDocument();
        doc.setKbId(kbId);
        doc.setDocName(docName);
        doc.setDocType("text");
        doc.setContent(content);
        doc.setWordCount(content.length());
        doc.setCreateBy(ShiroUtils.getLoginName());

        // 同步到Dify
        if (isNotEmpty(kb.getDifyDatasetId()) && isNotEmpty(kb.getDifyApiKey()))
        {
            try
            {
                JsonNode result = difyKnowledgeApi.createDocumentByText(
                    kb.getDifyBaseUrl(), kb.getDifyApiKey(), kb.getDifyDatasetId(),
                    docName, content
                );
                JsonNode docNode = result.path("document");
                doc.setDifyDocumentId(docNode.path("id").asText(""));
                doc.setIndexingStatus(docNode.path("indexing_status").asText("waiting"));
                String batch = result.path("batch").asText("");
                doc.setDifyBatch(batch);
                log.info("文档同步到Dify成功: docName={}, difyDocId={}", docName, doc.getDifyDocumentId());
            }
            catch (Exception e)
            {
                log.error("文档同步到Dify失败: docName={}", docName, e);
                doc.setIndexingStatus("error");
            }
        }

        knowledgeDocumentService.insertAiKnowledgeDocument(doc);
        // 更新知识库文档计数
        updateKbDocCount(kbId);
        return success("文档添加成功");
    }

    /**
     * 上传文件文档（同步到Dify）
     */
    @RequiresPermissions("ai:knowledge:add")
    @Log(title = "知识库文档", businessType = BusinessType.INSERT)
    @PostMapping("/document/uploadFile")
    @ResponseBody
    public AjaxResult uploadFileDocument(@RequestParam("kbId") Long kbId,
                                          @RequestParam("file") MultipartFile file)
    {
        AiKnowledgeBase kb = knowledgeBaseService.selectAiKnowledgeBaseByKbId(kbId);
        if (kb == null)
        {
            return error("知识库不存在");
        }

        AiKnowledgeDocument doc = new AiKnowledgeDocument();
        doc.setKbId(kbId);
        doc.setDocName(file.getOriginalFilename());
        doc.setDocType("file");
        doc.setFileSize(file.getSize());
        doc.setCreateBy(ShiroUtils.getLoginName());

        // 同步到Dify
        if (isNotEmpty(kb.getDifyDatasetId()) && isNotEmpty(kb.getDifyApiKey()))
        {
            try
            {
                JsonNode result = difyKnowledgeApi.createDocumentByFile(
                    kb.getDifyBaseUrl(), kb.getDifyApiKey(), kb.getDifyDatasetId(), file
                );
                JsonNode docNode = result.path("document");
                doc.setDifyDocumentId(docNode.path("id").asText(""));
                doc.setIndexingStatus(docNode.path("indexing_status").asText("waiting"));
                doc.setDifyBatch(result.path("batch").asText(""));
                log.info("文件文档同步到Dify成功: fileName={}", file.getOriginalFilename());
            }
            catch (Exception e)
            {
                log.error("文件文档同步到Dify失败: fileName={}", file.getOriginalFilename(), e);
                doc.setIndexingStatus("error");
            }
        }

        knowledgeDocumentService.insertAiKnowledgeDocument(doc);
        updateKbDocCount(kbId);
        return success("文件上传成功");
    }

    /**
     * 编辑文档（文本类型，同步到Dify）
     */
    @RequiresPermissions("ai:knowledge:edit")
    @Log(title = "知识库文档", businessType = BusinessType.UPDATE)
    @PostMapping("/document/edit")
    @ResponseBody
    public AjaxResult editDocumentSave(@RequestParam("docId") Long docId,
                                        @RequestParam("docName") String docName,
                                        @RequestParam(value = "content", required = false) String content)
    {
        AiKnowledgeDocument doc = knowledgeDocumentService.selectAiKnowledgeDocumentByDocId(docId);
        if (doc == null)
        {
            return error("文档不存在");
        }

        doc.setDocName(docName);
        if (content != null)
        {
            doc.setContent(content);
            doc.setWordCount(content.length());
        }
        doc.setUpdateBy(ShiroUtils.getLoginName());

        // 同步到Dify（文本类型且有difyDocumentId）
        if (isNotEmpty(doc.getDifyDocumentId()))
        {
            AiKnowledgeBase kb = knowledgeBaseService.selectAiKnowledgeBaseByKbId(doc.getKbId());
            if (kb != null && isNotEmpty(kb.getDifyDatasetId()) && isNotEmpty(kb.getDifyApiKey()))
            {
                try
                {
                    difyKnowledgeApi.updateDocumentByText(
                        kb.getDifyBaseUrl(), kb.getDifyApiKey(), kb.getDifyDatasetId(),
                        doc.getDifyDocumentId(), docName, content
                    );
                    doc.setIndexingStatus("indexing");
                    log.info("文档更新同步到Dify成功: docId={}, difyDocId={}", docId, doc.getDifyDocumentId());
                }
                catch (Exception e)
                {
                    log.error("文档更新同步到Dify失败: docId={}", docId, e);
                }
            }
        }

        knowledgeDocumentService.updateAiKnowledgeDocument(doc);
        return success("文档修改成功");
    }

    /**
     * 删除文档（同步Dify）
     */
    @RequiresPermissions("ai:knowledge:remove")
    @Log(title = "知识库文档", businessType = BusinessType.DELETE)
    @PostMapping("/document/remove")
    @ResponseBody
    public AjaxResult removeDocument(String ids)
    {
        Long[] docIds = convertStrToLongArray(ids);
        Long kbId = null;
        for (Long docId : docIds)
        {
            AiKnowledgeDocument doc = knowledgeDocumentService.selectAiKnowledgeDocumentByDocId(docId);
            if (doc != null)
            {
                kbId = doc.getKbId();
                // 删除Dify端文档
                if (isNotEmpty(doc.getDifyDocumentId()))
                {
                    AiKnowledgeBase kb = knowledgeBaseService.selectAiKnowledgeBaseByKbId(doc.getKbId());
                    if (kb != null && isNotEmpty(kb.getDifyDatasetId()) && isNotEmpty(kb.getDifyApiKey()))
                    {
                        try
                        {
                            difyKnowledgeApi.deleteDocument(kb.getDifyBaseUrl(), kb.getDifyApiKey(),
                                kb.getDifyDatasetId(), doc.getDifyDocumentId());
                        }
                        catch (Exception e)
                        {
                            log.warn("Dify文档删除失败（忽略）: difyDocId={}", doc.getDifyDocumentId());
                        }
                    }
                }
            }
        }
        int rows = knowledgeDocumentService.deleteAiKnowledgeDocumentByDocIds(docIds);
        if (kbId != null)
        {
            updateKbDocCount(kbId);
        }
        return toAjax(rows);
    }

    // ==================== Dify同步 ====================

    /**
     * 从Dify同步知识库列表到本地
     */
    @RequiresPermissions("ai:knowledge:edit")
    @Log(title = "知识库同步", businessType = BusinessType.UPDATE)
    @PostMapping("/syncFromDify")
    @ResponseBody
    public AjaxResult syncFromDify(@RequestParam("kbId") Long kbId)
    {
        AiKnowledgeBase kb = knowledgeBaseService.selectAiKnowledgeBaseByKbId(kbId);
        if (kb == null || !isNotEmpty(kb.getDifyApiKey()))
        {
            return error("知识库未配置Dify API");
        }

        try
        {
            // 同步文档列表
            JsonNode result = difyKnowledgeApi.listDocuments(
                kb.getDifyBaseUrl(), kb.getDifyApiKey(), kb.getDifyDatasetId(), 1, 100
            );
            JsonNode dataArr = result.path("data");
            int syncCount = 0;
            if (dataArr.isArray())
            {
                for (JsonNode docNode : dataArr)
                {
                    String difyDocId = docNode.path("id").asText("");
                    String docName = docNode.path("name").asText("未命名");
                    String indexingStatus = docNode.path("indexing_status").asText("waiting");
                    int wordCount = docNode.path("word_count").asInt(0);

                    // 检查本地是否已存在
                    AiKnowledgeDocument query = new AiKnowledgeDocument();
                    query.setKbId(kbId);
                    List<AiKnowledgeDocument> existing = knowledgeDocumentService.selectAiKnowledgeDocumentList(query);
                    boolean found = false;
                    for (AiKnowledgeDocument existDoc : existing)
                    {
                        if (difyDocId.equals(existDoc.getDifyDocumentId()))
                        {
                            // 更新状态
                            existDoc.setIndexingStatus(indexingStatus);
                            existDoc.setWordCount(wordCount);
                            existDoc.setDocName(docName);
                            knowledgeDocumentService.updateAiKnowledgeDocument(existDoc);
                            found = true;
                            break;
                        }
                    }
                    if (!found)
                    {
                        // 新增本地记录
                        AiKnowledgeDocument newDoc = new AiKnowledgeDocument();
                        newDoc.setKbId(kbId);
                        newDoc.setDocName(docName);
                        newDoc.setDocType("file");
                        newDoc.setDifyDocumentId(difyDocId);
                        newDoc.setIndexingStatus(indexingStatus);
                        newDoc.setWordCount(wordCount);
                        newDoc.setCreateBy(ShiroUtils.getLoginName());
                        knowledgeDocumentService.insertAiKnowledgeDocument(newDoc);
                        syncCount++;
                    }
                }
            }

            updateKbDocCount(kbId);
            return success("同步完成，新增" + syncCount + "个文档");
        }
        catch (Exception e)
        {
            log.error("从Dify同步失败: kbId={}", kbId, e);
            return error("同步失败: " + e.getMessage());
        }
    }

    /**
     * 刷新文档索引状态
     */
    @RequiresPermissions("ai:knowledge:edit")
    @PostMapping("/document/refreshStatus")
    @ResponseBody
    public AjaxResult refreshDocumentStatus(@RequestParam("docId") Long docId)
    {
        AiKnowledgeDocument doc = knowledgeDocumentService.selectAiKnowledgeDocumentByDocId(docId);
        if (doc == null || !isNotEmpty(doc.getDifyBatch()))
        {
            return error("文档信息不完整");
        }

        AiKnowledgeBase kb = knowledgeBaseService.selectAiKnowledgeBaseByKbId(doc.getKbId());
        if (kb == null || !isNotEmpty(kb.getDifyApiKey()))
        {
            return error("知识库未配置Dify API");
        }

        try
        {
            JsonNode result = difyKnowledgeApi.getIndexingStatus(
                kb.getDifyBaseUrl(), kb.getDifyApiKey(), kb.getDifyDatasetId(), doc.getDifyBatch()
            );
            JsonNode dataArr = result.path("data");
            if (dataArr.isArray() && dataArr.size() > 0)
            {
                JsonNode status = dataArr.get(0);
                doc.setIndexingStatus(status.path("indexing_status").asText("waiting"));
                knowledgeDocumentService.updateAiKnowledgeDocument(doc);
            }
            return success("状态已刷新");
        }
        catch (Exception e)
        {
            return error("刷新失败: " + e.getMessage());
        }
    }

    /**
     * 从Dify批量导入知识库（将Dify中已有的知识库导入到本地）
     */
    @RequiresPermissions("ai:knowledge:add")
    @Log(title = "知识库导入", businessType = BusinessType.INSERT)
    @PostMapping("/importFromDify")
    @ResponseBody
    public AjaxResult importFromDify(@RequestParam("difyBaseUrl") String difyBaseUrl,
                                      @RequestParam("difyApiKey") String difyApiKey)
    {
        if (!isNotEmpty(difyBaseUrl) || !isNotEmpty(difyApiKey))
        {
            return error("请填写Dify API地址和API Key");
        }

        try
        {
            // 获取Dify上所有知识库
            JsonNode result = difyKnowledgeApi.listDatasets(difyBaseUrl, difyApiKey, 1, 100);
            JsonNode dataArr = result.path("data");
            if (!dataArr.isArray() || dataArr.size() == 0)
            {
                return error("Dify中没有找到任何知识库");
            }

            // 获取本地已有的difyDatasetId列表，避免重复导入
            AiKnowledgeBase queryAll = new AiKnowledgeBase();
            List<AiKnowledgeBase> localList = knowledgeBaseService.selectAiKnowledgeBaseList(queryAll);
            java.util.Set<String> existingDifyIds = new java.util.HashSet<>();
            for (AiKnowledgeBase local : localList)
            {
                if (isNotEmpty(local.getDifyDatasetId()))
                {
                    existingDifyIds.add(local.getDifyDatasetId());
                }
            }

            int importCount = 0;
            int skipCount = 0;
            for (JsonNode dsNode : dataArr)
            {
                String difyDatasetId = dsNode.path("id").asText("");
                if (difyDatasetId.isEmpty() || existingDifyIds.contains(difyDatasetId))
                {
                    skipCount++;
                    continue;
                }

                String name = dsNode.path("name").asText("未命名知识库");
                String desc = dsNode.path("description").asText("");
                int docCount = dsNode.path("document_count").asInt(0);

                AiKnowledgeBase kb = new AiKnowledgeBase();
                kb.setKbName(name);
                kb.setKbDesc(desc);
                kb.setDifyDatasetId(difyDatasetId);
                kb.setDifyBaseUrl(difyBaseUrl);
                kb.setDifyApiKey(difyApiKey);
                kb.setDocumentCount(docCount);
                kb.setStatus("0");
                kb.setCreateBy(ShiroUtils.getLoginName());
                knowledgeBaseService.insertAiKnowledgeBase(kb);
                importCount++;

                log.info("从Dify导入知识库: name={}, difyDatasetId={}", name, difyDatasetId);
            }

            return success("导入完成！新增 " + importCount + " 个知识库" + (skipCount > 0 ? "，跳过 " + skipCount + " 个已存在" : ""));
        }
        catch (Exception e)
        {
            log.error("从Dify导入知识库失败", e);
            return error("导入失败: " + e.getMessage());
        }
    }

    // ==================== 工具方法 ====================

    private void updateKbDocCount(Long kbId)
    {
        int count = knowledgeDocumentService.countByKbId(kbId);
        AiKnowledgeBase update = new AiKnowledgeBase();
        update.setKbId(kbId);
        update.setDocumentCount(count);
        knowledgeBaseService.updateAiKnowledgeBase(update);
    }

    private boolean isNotEmpty(String str)
    {
        return str != null && !str.trim().isEmpty();
    }

    private Long[] convertStrToLongArray(String ids)
    {
        String[] strArr = ids.split(",");
        Long[] longArr = new Long[strArr.length];
        for (int i = 0; i < strArr.length; i++)
        {
            longArr[i] = Long.parseLong(strArr[i].trim());
        }
        return longArr;
    }
}
