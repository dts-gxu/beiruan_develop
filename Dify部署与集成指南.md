# Dify 部署与集成指南

## 一、部署信息

| 项目 | 值 |
|------|------|
| Dify版本 | 1.13.0 |
| 访问地址 | http://localhost:8180 |
| 部署目录 | `e:\beiruan_develop\dify-main\dify-main\docker` |
| 数据库 | PostgreSQL 15 (容器内 5432) |
| 向量数据库 | Weaviate 1.27.0 |
| 缓存 | Redis 6 |
| 端口 | Nginx: 8180 (HTTP), 8443 (HTTPS) |

## 二、首次初始化

1. 打开浏览器访问 **http://localhost:8180/install**
2. 设置管理员账号（邮箱+密码）
3. 登录后进入控制台

## 三、配置 DeepSeek 模型

1. 登录 Dify 控制台
2. 点击右上角头像 → **设置** → **模型供应商**
3. 找到 **DeepSeek** 点击配置（或搜索"DeepSeek"）
4. 填入：
   - **API Key**: `sk-583cca9088f34c308c30204131a96f4d`
   - **API Base URL**: `https://api.deepseek.com/v1`（如使用自定义端点才需填写）
5. 点击保存，系统会自动拉取可用模型列表
6. 在 **系统模型设置** 中将默认模型设为 `deepseek-chat`

## 四、创建 RAG 知识库

### 4.1 创建知识库
1. 左侧导航 → **知识库** → **创建知识库**
2. 名称：`RuoYi开发规范`
3. 描述：`RuoYi-Vue框架开发规范、代码结构、数据库设计规范等`

### 4.2 上传文档
建议上传以下文档（位于 `e:\beiruan_develop\` 目录）：
- `AI开发流程功能设计说明书.md` — 9步AI开发流程定义
- `第一步需求分析详细执行手册.md` — 需求分析详细步骤
- RuoYi框架相关规范文档（如有）

### 4.3 知识库设置
- **分段模式**: 自动分段
- **索引方式**: 高质量（使用Embedding模型）
- **检索模式**: 混合检索（关键词+语义）

## 五、创建 AI 开发工作流

### 5.1 创建工作流应用
1. 首页 → **创建应用** → **工作流**
2. 名称：`AI软件开发全流程`
3. 在工作流编辑器中，按以下步骤编排节点：

```
开始 → 步骤1.3 关键信息提取 → 步骤1.4 RuoYi适配分析 → 步骤1.5 完整性评估
→ 步骤1.6 生成报告 → 步骤1.7 结构化JSON → 结束
```

### 5.2 每个LLM节点配置
- **模型**: deepseek-chat
- **上下文**: 关联"RuoYi开发规范"知识库
- **系统提示词**: 参照 `AiChatService.java` 中各步骤的 prompt

### 5.3 发布并获取 API

1. 工作流编辑完成后点击 **发布**
2. 左侧 **访问API** → 获取：
   - **API Base URL**: `http://localhost:8180/v1`
   - **API Key**: （点击创建API密钥）
3. 工作流运行接口：
   ```
   POST http://localhost:8180/v1/workflows/run
   Authorization: Bearer {api_key}
   Content-Type: application/json
   
   {
     "inputs": {
       "document": "需求文档内容..."
     },
     "response_mode": "blocking",
     "user": "ruoyi-admin"
   }
   ```

## 六、RuoYi 集成架构

```
┌─────────────┐     HTTP API      ┌──────────────┐
│  RuoYi Web  │ ───────────────→  │  Dify Server │
│  (chat.html)│                   │  :8180       │
│             │ ←───────────────  │              │
│  Controller │   JSON Response   │  Workflow    │
│  Service    │                   │  + RAG       │
└─────────────┘                   │  + DeepSeek  │
                                  └──────────────┘
```

## 七、常用命令

```bash
# 启动
cd e:\beiruan_develop\dify-main\dify-main\docker
docker compose up -d

# 停止
docker compose down

# 查看状态
docker compose ps

# 查看日志
docker compose logs -f api
docker compose logs -f worker

# 重启单个服务
docker compose restart api
```

## 八、注意事项

1. **端口冲突**: Dify使用8180端口，RuoYi使用80/8080端口，互不冲突
2. **数据持久化**: 所有数据保存在 `docker/volumes/` 目录
3. **API调用**: RuoYi后端通过HTTP调用Dify API，无需额外SDK
4. **知识库更新**: 当RuoYi规范变更时，在Dify知识库中更新文档即可
