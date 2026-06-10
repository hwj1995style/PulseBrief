# PulseBrief Admin 每日简报生成与审核 V1 设计

## 背景与现状

PulseBrief 当前已经完成“真实资讯采集 → 候选资讯 → Admin 审核发布 → Flutter APP 文章展示”的基础闭环。用户端已有 `daily_digest` 表、`GET /api/digests/today` 和 `GET /api/digests/{id}`，Flutter 简报页与播放器页也已通过 `ApiPulseRepository` 接入用户端简报 API。

当前缺口是简报仍主要依赖种子数据，运营人员无法在 Admin 中基于已发布文章创建、编辑和发布每日简报。阶段 18 的目标是补齐 Admin 简报生成与审核能力，让“已发布文章”进一步组织成“今日全球早报、午间快讯、晚间复盘、AI 专题、投行观点精选”等用户端可读简报。

## 目标与非目标

目标：

1. 设计 Admin Digest API，用于查询、创建、编辑和发布每日简报。
2. 支持从已发布 `news_article` 中选择简报条目。
3. 支持编辑简报标题、副标题、摘要、热点清单和播报文案。
4. 发布后 Flutter 通过现有 `/api/digests/today` 和 `/api/digests/{id}` 读取。
5. V1 保持人工审核优先，不自动发布。
6. 将简报发布链路纳入后端和 Flutter Repository 回归。

非目标：

1. 不实现真实 TTS 音频文件生成。
2. 不实现 AI 自动生成摘要服务。
3. 不实现推送任务投递。
4. 不实现复杂排班、多版本回滚或多人协同编辑。
5. 不实现真实研报 PDF 下载或全文转载。
6. 不改 Flutter 简报页 UI 视觉。

## 影响范围

后端：

- 新增 Admin Digest Controller、DTO 和 Application Service。
- 扩展 `DailyDigest` 实体写入能力。
- 必要时新增简报与文章关联表，用于记录简报条目来源。
- 保持用户端 `DigestController` 响应结构兼容。

React Admin：

- 后续新增“简报管理”页面。
- 页面可从已发布文章池中选择条目，并编辑最终简报内容。

Flutter：

- 不新增页面。
- 继续通过 `PulseRepository.getTodayDigest()` 和 `getDigestDetail()` 读取发布结果。
- 增加 Repository 测试，验证 Admin 发布后的简报响应可映射。

文档：

- 更新下一阶段任务清单、真实资讯采集与 Admin 审核发布总清单、测试方案。

## 数据模型与状态模型

### 现有 `daily_digest`

现有字段：

| 字段 | 说明 |
| --- | --- |
| `id` | 每日简报 ID |
| `digest_date` | 简报日期 |
| `digest_type` | 简报类型：`MORNING/NOON/EVENING/AI/INVESTMENT_VIEW` |
| `category_code` | 分类编码，可为空 |
| `title` | 简报标题 |
| `summary` | 简报摘要 |
| `content` | 热点清单或简报要点，按换行存储 |
| `audio_text` | 播报文案 |
| `digest_status` | 简报状态 |
| `publish_time` | 发布时间 |
| `created_at` | 创建时间 |
| `updated_at` | 更新时间 |

### 建议新增 `daily_digest_article`

为保留简报与已发布文章的来源关系，建议新增关联表：

| 字段 | 说明 |
| --- | --- |
| `id` | 主键 |
| `digest_id` | 关联 `daily_digest.id` |
| `article_id` | 关联 `news_article.id` |
| `sort_no` | 简报内排序 |
| `highlight_text` | 该文章在简报中的热点文案 |
| `created_at` | 创建时间 |

约束：

1. `digest_id + article_id` 唯一，避免同一文章重复加入同一简报。
2. 用户端 V1 不直接返回该关联表，仍返回 `daily_digest.content` 拆分后的 `highlights`。
3. 后续若要做“简报中每条热点跳转原文”，可从该表扩展。

### 状态流转

```text
DRAFT
→ PUBLISHED
→ OFFLINE
```

规则：

1. `DRAFT`：草稿，可编辑、发布，用户端不可见。
2. `PUBLISHED`：已发布，用户端可见。
3. `OFFLINE`：已下线，用户端不可见。
4. 同一天同类型建议只允许一个 `PUBLISHED` 简报；发布新版本时，可先将旧版本下线或返回 `409 CONFLICT`。V1 推荐返回 `409`，避免隐式替换。

## Admin API 设计

所有接口路径均在 `/api/admin/**` 下，沿用 Admin Token：

```text
Authorization: Bearer dev-admin-token
```

### 可选文章池

```text
GET /api/admin/digests/article-candidates
```

查询参数：

| 参数 | 说明 |
| --- | --- |
| `categoryCode` | 分类编码，默认 `all` |
| `keyword` | 标题、摘要、来源关键字，可选 |
| `page` | 页码，默认 1 |
| `pageSize` | 每页数量，默认 20，最大 50 |

响应：

```json
{
  "items": [
    {
      "id": 101,
      "title": "高盛：AI 基建投资仍将持续",
      "sourceName": "Goldman Sachs Research",
      "publishTime": "2026-06-10T09:30:00+08:00",
      "categoryCode": "investment_view",
      "categoryName": "投行观点",
      "summary": "AI 基础设施投资仍处于扩张阶段。"
    }
  ],
  "page": 1,
  "pageSize": 20,
  "total": 1,
  "hasMore": false
}
```

规则：

1. 只返回 `article_status = PUBLISHED` 的文章。
2. 默认按发布时间倒序。
3. V1 不返回新闻全文。

### 简报列表

```text
GET /api/admin/digests
```

查询参数：

| 参数 | 说明 |
| --- | --- |
| `digestDate` | 简报日期，可选 |
| `digestType` | 简报类型，可选 |
| `status` | `DRAFT/PUBLISHED/OFFLINE`，可选 |
| `page` | 页码 |
| `pageSize` | 每页数量 |

响应：

```json
{
  "items": [
    {
      "id": 1,
      "digestDate": "2026-06-10",
      "digestType": "MORNING",
      "title": "今日全球早报",
      "summary": "精选全球、财经、AI 与投行观点 10 条重点",
      "status": "PUBLISHED",
      "publishTime": "2026-06-10T08:30:00+08:00",
      "articleCount": 6
    }
  ],
  "page": 1,
  "pageSize": 20,
  "total": 1,
  "hasMore": false
}
```

### 简报详情

```text
GET /api/admin/digests/{id}
```

响应：

```json
{
  "id": 1,
  "digestDate": "2026-06-10",
  "digestType": "MORNING",
  "categoryCode": "global",
  "title": "今日全球早报",
  "summary": "每天几分钟，听懂全球重点",
  "content": "英伟达 Blackwell Ultra 发布\n美联储维持利率不变",
  "audioText": "欢迎收听脉闻今日全球早报。第一条，英伟达发布新一代 AI 芯片...",
  "status": "DRAFT",
  "publishTime": null,
  "articles": [
    {
      "articleId": 101,
      "sortNo": 1,
      "highlightText": "英伟达 Blackwell Ultra 发布",
      "title": "英伟达推出新一代 AI 芯片 Blackwell Ultra",
      "sourceName": "Tech Brief"
    }
  ],
  "availableActions": ["EDIT", "PUBLISH"]
}
```

### 创建简报草稿

```text
POST /api/admin/digests
```

请求：

```json
{
  "digestDate": "2026-06-10",
  "digestType": "MORNING",
  "categoryCode": "global",
  "title": "今日全球早报",
  "summary": "精选全球、财经、AI 与投行观点 10 条重点",
  "content": "英伟达 Blackwell Ultra 发布\n美联储维持利率不变",
  "audioText": "欢迎收听脉闻今日全球早报...",
  "articles": [
    {
      "articleId": 101,
      "sortNo": 1,
      "highlightText": "英伟达 Blackwell Ultra 发布"
    }
  ]
}
```

规则：

1. 创建后状态为 `DRAFT`。
2. `digestDate`、`digestType`、`title` 不能为空。
3. `articles` 至少 1 条，最多 10 条。
4. 选择的文章必须为 `PUBLISHED`。
5. `content` 可由 `articles.highlightText` 自动拼接兜底；V1 不接入 AI 自动生成。

### 编辑简报草稿

```text
PUT /api/admin/digests/{id}
```

规则：

1. 仅 `DRAFT` 可编辑。
2. 已发布简报不可直接编辑；后续可做复制为新草稿。
3. 更新时先替换该简报的文章关联，再写入新的排序和热点文案。

### 发布简报

```text
POST /api/admin/digests/{id}/publish
```

请求：

```json
{
  "publishNow": true
}
```

规则：

1. 仅 `DRAFT` 可发布。
2. 同一天同类型已有 `PUBLISHED` 时返回 `409 CONFLICT`。
3. 发布时写入 `publish_time`，状态变为 `PUBLISHED`。
4. 发布后用户端 `/api/digests/today` 和 `/api/digests/{id}` 可读取。
5. V1 不生成真实音频文件，`audioText` 作为客户端播报文案。

### 下线简报

```text
POST /api/admin/digests/{id}/offline
```

规则：

1. 仅 `PUBLISHED` 可下线。
2. 下线后用户端不可见。
3. V1 不删除简报。

## 后端实现方案

建议新增：

```text
backend/src/main/java/com/pulsebrief/admin/api/AdminDigestController.java
backend/src/main/java/com/pulsebrief/admin/api/AdminDigestCreateRequest.java
backend/src/main/java/com/pulsebrief/admin/api/AdminDigestUpdateRequest.java
backend/src/main/java/com/pulsebrief/admin/api/AdminDigestPublishRequest.java
backend/src/main/java/com/pulsebrief/admin/api/AdminDigestResponse.java
backend/src/main/java/com/pulsebrief/admin/api/AdminDigestDetailResponse.java
backend/src/main/java/com/pulsebrief/admin/api/AdminDigestArticleCandidateResponse.java
backend/src/main/java/com/pulsebrief/admin/service/AdminDigestApplicationService.java
backend/src/main/java/com/pulsebrief/digest/domain/DailyDigestArticle.java
backend/src/main/java/com/pulsebrief/digest/repository/DailyDigestArticleRepository.java
```

`DailyDigest` 需要补充：

1. 构造方法或静态工厂用于创建草稿。
2. `updateDraft(...)` 方法。
3. `publish(LocalDateTime publishTime)` 方法。
4. `offline()` 方法。
5. `getDigestStatus()`、`getCategoryCode()` 等缺失 getter。

Migration 建议：

```text
V7__daily_digest_admin_review.sql
```

内容：

1. 新增 `daily_digest_article`。
2. 补充 `daily_digest` 的唯一索引或普通索引：
   - `idx_digest_status_date_type (digest_status, digest_date, digest_type)`
   - 不建议在数据库层做 `PUBLISHED` 部分唯一索引，MySQL 实现复杂；V1 在服务层做冲突校验。

## React Admin 影响

阶段 18 后端 API 完成后，React Admin 可新增“简报管理”页面：

1. 简报列表：按日期、类型、状态筛选。
2. 创建草稿：选择日期和类型。
3. 文章池：从已发布文章中加入简报。
4. 编辑区：标题、摘要、热点清单、播报文案。
5. 发布按钮：发布后提示成功，用户端可读。

V1 页面可以先做基础表单，不做复杂拖拽；排序用上移/下移按钮或数字输入。

## Flutter 影响

Flutter 不需要新增接口，只需要回归：

1. `/api/digests/today` 返回新发布简报。
2. `/api/digests/{id}` 返回播报文案和热点列表。
3. `ApiPulseRepository` 可映射 `TodayDigestFeed` 和 `Digest`。
4. 播放器页仍使用现有 UI 与客户端播报文案。

## 测试与回归方案

后端测试：

1. Admin Token 缺失访问 `/api/admin/digests/**` 返回 `401`。
2. 可选文章池只返回已发布文章。
3. 创建草稿后 `daily_digest.digest_status = DRAFT`。
4. 创建草稿时，未发布文章不可加入。
5. 编辑草稿可替换标题、摘要、热点和文章关联。
6. 发布草稿后用户端 `/api/digests/today` 可读。
7. 发布草稿后用户端 `/api/digests/{id}` 可读。
8. 同一天同类型重复发布返回 `409`。
9. 下线后用户端不再读取该简报。
10. OpenAPI 文档包含 `/api/admin/digests`。

Flutter 测试：

1. `ApiPulseRepository.getTodayDigest()` 映射 Admin 发布后的 `headline/digests/highlights`。
2. `ApiPulseRepository.getDigestDetail()` 映射 `audioText/content`。
3. 现有 `DigestPage` widget 导航不变。

验证命令：

```powershell
cd backend
.\mvnw.cmd test

cd ..\mobile
flutter analyze
flutter test

cd ..\admin
npm test -- --run
npm run lint
npm run build
```

## 风险与分阶段落地建议

风险：

1. 如果不记录简报与文章关联，后续无法追踪每条热点来源。
2. 如果允许已发布简报直接编辑，用户端内容会不可追溯。
3. 如果发布时隐式替换同日同类型简报，运营可能误覆盖当天早报。
4. 如果 `content` 和 `articles.highlightText` 不一致，用户端热点清单可能与 Admin 选择不一致。

分阶段建议：

1. 第一轮先实现后端 Admin Digest API 和用户端读取回归。
2. 第二轮实现 React Admin 简报管理页面。
3. 第三轮补 Flutter 简报页真实发布截图验收。
4. 后续再考虑 AI 辅助生成、TTS 音频、推送和版本回滚。
