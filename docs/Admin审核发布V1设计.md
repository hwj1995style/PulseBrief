# PulseBrief Admin 审核发布 V1 设计

## 背景与现状

PulseBrief 已完成用户端 V1 API、真实资讯采集适配层、原始资讯入库、去重和候选资讯生成。当前真实资讯链路已经可以将外部公开资讯映射为 `raw_news_item`，再生成 `candidate_article`，但候选内容仍不能被运营人员审核、拒绝或发布到用户端。

下一阶段需要先设计 Admin API，而不是直接创建 React Admin 页面。原因是后台 UI、OpenAPI 文档、权限控制和发布事务都依赖稳定的后端边界。V1 仍坚持人工审核优先，所有真实采集内容必须经过 Admin 审核后，才可以写入 `news_article` 并被 Flutter 用户端读取。

## 目标与非目标

目标：

1. 设计 Admin V1 API 的资源边界、路径、请求响应和状态流转。
2. 设计候选资讯列表、详情、编辑、拒绝和发布为文章的后端契约。
3. 设计采集任务、原始资讯、候选资讯、文章和简报的 Admin 查询边界。
4. 设计开发态 Admin Token 鉴权方案，和用户端 Bearer Token 隔离。
5. 设计 OpenAPI Admin 分组，方便后续 React Admin 直接对接。
6. 设计发布事务、幂等规则和用户端发布隔离测试。

非目标：

1. 本阶段不实现 React Admin 页面。
2. 本阶段不实现复杂 RBAC、多管理员角色或组织权限。
3. 本阶段不接入真实登录、短信、邮箱或 SSO。
4. 本阶段不实现自动发布。
5. 本阶段不实现真实 AI 摘要服务。
6. 本阶段不实现研报 PDF 下载、全文转载或付费内容采集。
7. 本阶段不实现生产级操作审计表，操作日志可在阶段 19 补齐。

## 影响范围

后端：

1. 新增 `com.pulsebrief.ingestion.api` 下的 Admin Controller。
2. 新增 Admin 鉴权拦截或过滤逻辑。
3. 新增候选资讯查询、更新、拒绝、发布服务。
4. 新增发布为 `news_article` 的事务服务。
5. OpenAPI 增加 `admin-v1` 分组。

Flutter：

1. 不新增页面。
2. 不读取 Admin API。
3. 继续只读取 `article_status = PUBLISHED` 的用户端文章。
4. 发布完成后，用户端可通过现有首页、分类和详情接口看到新文章。

React Admin：

1. 后续可按本文 API 直接建立 API client。
2. 首批页面为采集任务、原始资讯、候选资讯、文章管理和简报管理。
3. UI 阶段只调用 `/api/admin/**`，不直接调用用户端 `/api/**` 写接口。

部署与配置：

1. 开发态 Admin Token 使用环境变量配置。
2. 未配置时使用本地默认值，方便开发，但生产必须覆盖。
3. Admin API 默认可在本地 Swagger 中查看。

## 数据模型与状态模型

### 现有表

`candidate_article`：

1. `PENDING_REVIEW`：待审核，可编辑、拒绝、发布。
2. `REJECTED`：已拒绝，不进入用户端。
3. `PUBLISHED`：已发布，关联 `published_article_id`。

`raw_news_item`：

1. `NEW`：已入库但未生成候选。
2. `CANDIDATE`：已生成候选。
3. `REJECTED`：候选被拒绝后同步标记。
4. `PUBLISHED`：候选发布后同步标记。
5. `DUPLICATE`、`ERROR`：采集或去重阶段使用。

`news_article`：

1. `PENDING`：草稿或未发布内容。
2. `PUBLISHED`：用户端可见。
3. `OFFLINE`：下线内容，用户端不可见。

### 发布字段映射

候选发布为文章时，字段映射如下：

| 目标字段 | 来源 |
| --- | --- |
| `news_article.title` | `candidate_article.title` |
| `news_article.summary` | `candidate_article.summary` |
| `news_article.ai_summary` | 发布请求中的 `aiSummary`，为空时使用候选摘要 |
| `news_article.key_points` | 发布请求中的 `keyPoints`，按换行存储 |
| `news_article.impact_analysis` | 发布请求中的 `impactAnalysis` |
| `news_article.source_name` | `candidate_article.source_name` |
| `news_article.original_url` | `candidate_article.original_url` |
| `news_article.category_code` | `candidate_article.category_code` |
| `news_article.publish_time` | Admin 发布时间，默认当前时间 |
| `news_article.article_hash` | `candidateId + originalUrl` 或规范化标题与链接哈希 |
| `news_article.article_status` | `PUBLISHED` |

V1 不写新闻全文，不生成真实音频文件，不下载远端图片。图片后续可在文章卡片扩展字段中设计。

## 权限模型

### Admin Token

开发态 Admin API 使用独立 Token：

```text
Authorization: Bearer dev-admin-token
```

配置项：

```text
PULSEBRIEF_ADMIN_TOKEN=dev-admin-token
```

规则：

1. `/api/admin/**` 必须校验 Admin Token。
2. 用户端 `dev-token-1` 不能访问 Admin API。
3. Admin Token 不赋予用户端收藏、订阅、历史等用户身份能力。
4. 缺失或错误 Token 返回 `401 UNAUTHORIZED`。
5. 权限不足返回 `403 FORBIDDEN`，V1 暂不细分角色。

### OpenAPI

OpenAPI 建议拆分两个分组：

1. `mobile-v1`：现有用户端接口。
2. `admin-v1`：`/api/admin/**`。

Swagger UI 中 Admin 接口需要明确标注：

```text
Admin API requires Authorization: Bearer <admin-token>
```

## Admin API 设计

统一响应结构沿用现有 `ApiResponse<T>`：

```json
{
  "code": "OK",
  "message": "success",
  "data": {}
}
```

分页响应建议使用：

```json
{
  "items": [],
  "page": 1,
  "pageSize": 20,
  "total": 100
}
```

### 采集任务

#### 查询采集任务列表

```text
GET /api/admin/ingestion/jobs
```

查询参数：

| 参数 | 说明 |
| --- | --- |
| `sourceCode` | 采集源编码，可选 |
| `status` | 任务状态，可选 |
| `page` | 页码，默认 1 |
| `pageSize` | 每页条数，默认 20，最大 100 |

响应字段：

```json
{
  "items": [
    {
      "id": 1,
      "sourceCode": "fixture-global",
      "triggerType": "MANUAL",
      "status": "SUCCESS",
      "startedAt": "2026-06-09T09:00:00+08:00",
      "finishedAt": "2026-06-09T09:00:03+08:00",
      "fetchedCount": 10,
      "newCount": 8,
      "duplicateCount": 2,
      "candidateCount": 8,
      "errorMessage": null
    }
  ],
  "page": 1,
  "pageSize": 20,
  "total": 1
}
```

#### 手动触发候选生成

```text
POST /api/admin/ingestion/candidates/generate
```

请求体：

```json
{
  "sourceCode": "fixture-global",
  "limit": 50
}
```

响应：

```json
{
  "scannedCount": 8,
  "generatedCount": 8
}
```

说明：

1. V1 不直接触发真实外部采集，真实 Provider 稳定后再补。
2. `limit` 最大 100，避免一次生成过多候选。

### 原始资讯

#### 查询原始资讯池

```text
GET /api/admin/ingestion/raw-items
```

查询参数：

| 参数 | 说明 |
| --- | --- |
| `sourceCode` | 采集源编码 |
| `status` | `NEW/CANDIDATE/REJECTED/PUBLISHED/DUPLICATE/ERROR` |
| `keyword` | 标题或来源关键字 |
| `page` | 页码 |
| `pageSize` | 每页条数 |

响应字段：

```json
{
  "items": [
    {
      "id": 101,
      "sourceCode": "fixture-global",
      "title": "AI infrastructure demand remains strong",
      "summary": "Public market commentary highlights continued demand.",
      "sourceName": "Example Markets",
      "originalUrl": "https://example.com/ai",
      "publishedAt": "2026-06-09T09:00:00+08:00",
      "fetchedAt": "2026-06-09T09:01:00+08:00",
      "status": "CANDIDATE",
      "contentHash": "..."
    }
  ],
  "page": 1,
  "pageSize": 20,
  "total": 1
}
```

### 候选资讯

#### 查询候选列表

```text
GET /api/admin/candidates
```

查询参数：

| 参数 | 说明 |
| --- | --- |
| `status` | 默认 `PENDING_REVIEW` |
| `categoryCode` | 分类编码 |
| `keyword` | 标题、摘要或来源关键字 |
| `page` | 页码 |
| `pageSize` | 每页条数 |

响应字段：

```json
{
  "items": [
    {
      "id": 201,
      "rawNewsItemId": 101,
      "title": "AI infrastructure demand remains strong",
      "summary": "Public market commentary highlights continued demand.",
      "categoryCode": "ai",
      "sourceName": "Example Markets",
      "originalUrl": "https://example.com/ai",
      "publishedAt": "2026-06-09T09:00:00+08:00",
      "status": "PENDING_REVIEW",
      "createdAt": "2026-06-09T09:02:00+08:00"
    }
  ],
  "page": 1,
  "pageSize": 20,
  "total": 1
}
```

#### 查询候选详情

```text
GET /api/admin/candidates/{id}
```

详情需要返回：

1. 候选字段。
2. 原始资讯字段。
3. 原文链接。
4. 可能重复信息，V1 可先返回空数组。
5. 当前可执行动作：`EDIT`、`REJECT`、`PUBLISH`。

#### 编辑候选

```text
PUT /api/admin/candidates/{id}
```

请求体：

```json
{
  "title": "高盛：AI 基建投资仍将持续",
  "summary": "高盛公开观点认为，AI 基础设施投资仍处扩张阶段。",
  "categoryCode": "investment_view"
}
```

规则：

1. 仅 `PENDING_REVIEW` 可编辑。
2. 标题不能为空。
3. 摘要不应超过 2000 字符。
4. 分类必须存在于 `news_category`。

#### 拒绝候选

```text
POST /api/admin/candidates/{id}/reject
```

请求体：

```json
{
  "reviewNote": "来源质量不足，暂不发布"
}
```

状态变化：

```text
candidate_article.PENDING_REVIEW -> REJECTED
raw_news_item.CANDIDATE -> REJECTED
```

规则：

1. 仅 `PENDING_REVIEW` 可拒绝。
2. 重复拒绝返回 `409 CONFLICT`。
3. 拒绝后用户端不可见。

#### 发布候选为文章

```text
POST /api/admin/candidates/{id}/publish
```

请求体：

```json
{
  "title": "高盛：AI 基建投资仍将持续",
  "summary": "高盛公开观点认为，AI 基础设施投资仍处扩张阶段。",
  "aiSummary": "AI 基建投资仍处于扩张阶段，算力、电力和数据中心产业链受益。",
  "keyPoints": [
    "AI 基础设施投资继续扩张。",
    "数据中心和电力需求受到关注。",
    "云厂商 CapEx 可能继续上行。"
  ],
  "impactAnalysis": "相关板块可能受到市场关注，但仍需警惕估值波动。",
  "categoryCode": "investment_view",
  "publishNow": true
}
```

状态变化：

```text
candidate_article.PENDING_REVIEW -> PUBLISHED
raw_news_item.CANDIDATE -> PUBLISHED
news_article.article_status = PUBLISHED
candidate_article.published_article_id = news_article.id
```

事务规则：

1. 创建 `news_article`、更新候选状态、更新原始资讯状态必须在同一事务中完成。
2. 发布请求必须幂等保护：已发布候选再次发布返回 `409 CONFLICT`，不重复创建文章。
3. `article_hash` 使用稳定算法生成，避免同一候选重复发布。
4. 发布后用户端现有文章 API 可读取新文章。

### 文章管理

#### 查询文章列表

```text
GET /api/admin/articles
```

参数：

| 参数 | 说明 |
| --- | --- |
| `status` | `PENDING/PUBLISHED/OFFLINE` |
| `categoryCode` | 分类编码 |
| `keyword` | 标题或来源 |
| `page` | 页码 |
| `pageSize` | 每页条数 |

#### 下线文章

```text
POST /api/admin/articles/{id}/offline
```

状态变化：

```text
news_article.PUBLISHED -> OFFLINE
```

规则：

1. 下线后用户端不可见。
2. V1 不删除文章，保留可追溯记录。

### 简报管理

阶段 15 只设计边界，实际实现留到阶段 18。

```text
GET  /api/admin/digests
GET  /api/admin/digests/{id}
POST /api/admin/digests
PUT  /api/admin/digests/{id}
POST /api/admin/digests/{id}/publish
```

V1 简报发布仍基于人工选择已发布文章，不自动生成真实音频文件。

## 后端实现方案

建议包结构：

```text
com.pulsebrief.admin
├── api
├── security
└── service

com.pulsebrief.ingestion.api
com.pulsebrief.ingestion.service
```

核心类建议：

1. `AdminSecurityFilter`：校验 `/api/admin/**` Admin Token。
2. `AdminCandidateController`：候选列表、详情、编辑、拒绝、发布。
3. `AdminIngestionController`：采集任务、原始资讯、候选生成。
4. `AdminArticleController`：文章列表和下线。
5. `AdminCandidateApplicationService`：候选审核发布事务。
6. `AdminPageResponse<T>`：统一分页响应。
7. `AdminOpenApiConfig`：Admin API 分组。

错误码建议：

| 场景 | HTTP 状态 |
| --- | --- |
| 未登录 Admin | `401` |
| Token 不是 Admin Token | `403` |
| 候选不存在 | `404` |
| 状态不允许操作 | `409` |
| 请求字段非法 | `400` |

## 前端影响

Flutter：

1. 不调用 Admin API。
2. 发布后通过既有 `/api/articles/home`、`/api/articles`、`/api/articles/{id}` 读取新内容。
3. 不展示候选、原始资讯和审核备注。

React Admin：

1. 登录页先使用本地 Admin Token 输入或 `.env` 注入。
2. API client 统一追加 `Authorization: Bearer <admin-token>`。
3. 候选列表支持状态、分类和关键字筛选。
4. 候选详情提供编辑、拒绝、发布三个主操作。
5. 发布成功后跳转文章详情或候选列表。

## 测试与回归方案

后端测试：

1. Admin Token 缺失访问 `/api/admin/**` 返回 `401`。
2. 用户端 Token 访问 Admin API 返回 `403`。
3. Admin Token 可查询候选列表。
4. 编辑候选只允许 `PENDING_REVIEW` 状态。
5. 拒绝候选后，候选和 raw 状态同步更新。
6. 发布候选后创建 `news_article`，候选和 raw 状态同步更新。
7. 重复发布同一候选返回 `409`，不重复创建文章。
8. 发布后的文章可被用户端文章 API 读取。
9. 拒绝的候选不会出现在用户端。
10. OpenAPI 中存在 `admin-v1` 分组。

回归命令：

```powershell
cd backend
..\scripts\use-jdk17.ps1
.\mvnw.cmd test
```

后续 Admin 工程创建后补充：

```powershell
cd admin
npm run lint
npm run test
npm run build
```

## 风险与分阶段落地建议

风险：

1. 发布事务如果不加状态校验，可能重复创建文章。
2. Admin Token 若与用户端 Token 混用，会造成权限边界不清。
3. 候选编辑字段如果不校验分类和标题，会影响用户端展示质量。
4. 文章下线如果只改 Admin 状态，不同步用户端查询条件，会造成已下线内容仍可见。
5. OpenAPI 分组如果不拆分，后续 React Admin 对接成本会升高。

分阶段落地：

1. 先实现 Admin Token 和候选列表/详情查询。
2. 再实现候选编辑和拒绝。
3. 再实现候选发布为 `news_article`。
4. 最后补文章下线、OpenAPI Admin 分组和完整回归。

## 下一阶段实施顺序

建议 Stage 15 实现时按以下顺序推进：

1. 增加 Admin 鉴权测试和 `AdminSecurityFilter`。
2. 增加候选列表查询测试和 Controller。
3. 增加候选详情查询测试。
4. 增加候选编辑测试。
5. 增加候选拒绝测试。
6. 增加候选发布测试。
7. 增加 OpenAPI Admin 分组测试。
8. 更新 README 和 Swagger 使用说明。
