# Admin 手动触发真实 Provider 与单源配置接入设计

## 背景与现状

PulseBrief 已完成真实资讯采集基础链路、Admin 采集监控、采集源启停、真实 Provider 环境护栏和 RSS Provider V1。当前阶段 23 的 RSS Provider 已能解析真实 RSS/Atom 元数据，但仍存在三个缺口：

1. Admin 只能查看采集任务、失败日志、今日指标和采集源状态，不能从页面手动触发某个采集源。
2. RSS Provider 目前可通过 live smoke 或环境变量验证真实外网采集，但还没有正式接入 `news_ingestion_source.base_url` 的单源配置。
3. 当前失败日志只覆盖入库任务已有流程；如果 Provider 拉取或解析阶段失败，需要先创建任务日志，再记录失败原因，才能形成完整闭环。

阶段 24 的目标是把“采集源配置 -> Provider 执行 -> 原始资讯入库 -> 候选生成 -> 任务日志 -> Admin 展示”串成一个可控的手动触发闭环。仍然保持真实采集默认关闭，不做定时调度、不做全文抓取、不下载 PDF、不调用 AI 摘要。

## 目标与非目标

目标：

1. 新增 Admin 手动触发单个采集源的后端接口。
2. 让 RSS Provider 正式读取 `news_ingestion_source.base_url`，不再依赖运行时环境变量作为主路径。
3. Provider 执行失败时也必须写入 `news_ingestion_job`，并在 Admin 任务列表展示失败原因。
4. 手动触发成功后，完成原始资讯入库、去重统计和候选生成统计。
5. Admin 页面支持对单个采集源点击“手动采集”，展示执行中、成功和失败状态，并刷新任务列表与指标。
6. 继续保留 CI fixture-only 策略，不让默认自动化请求真实外网。

非目标：

1. 不做定时任务调度和后台队列。
2. 不做批量一键采集全部来源。
3. 不做历史回溯、翻页抓取或归档同步。
4. 不新增全文抓取、PDF 下载、AI 摘要或复杂分类。
5. 不新增生产级重试、告警和任务取消能力。
6. 不允许 Admin 输入任意 URL 发起采集；只能触发数据库中已配置且通过护栏的采集源。

## 影响范围

后端：

1. 新增手动触发接口：`POST /api/admin/ingestion/sources/{id}/run`。
2. 新增采集运行编排服务，负责 Provider 选择、任务日志生命周期、入库和候选生成。
3. 扩展 Provider 调用边界，使 Provider 可以读取 `NewsIngestionSource` 配置。
4. 补充 `NewsIngestionSource` 的 `baseUrl`、`rateLimitPerHour` 等只读 getter。
5. 补充后端 Controller/Service 测试。

Admin：

1. 采集源卡片新增“手动采集”按钮。
2. 点击后禁用当前按钮，显示运行状态。
3. 完成后刷新采集任务、今日指标、异常数据和采集源列表。
4. 失败时显示后端返回的错误信息，并可在任务列表中看到对应失败任务。

配置与部署：

1. 不新增必需环境变量。
2. `PULSEBRIEF_INGESTION_ENABLED` 仍默认关闭；Admin 手动触发必须在后端配置允许时才执行。
3. 真实 RSS URL 继续由 `.env.local` 或数据库采集源配置控制，不提交真实私有配置。

## 数据模型或权限模型

本阶段不新增数据库表，优先复用现有字段：

| 表 | 字段 | 用途 |
| --- | --- | --- |
| `news_ingestion_source` | `code` | 手动触发时的 sourceCode |
| `news_ingestion_source` | `provider_type` | Provider 路由，例如 `FIXTURE`、`RSS` |
| `news_ingestion_source` | `base_url` | RSS feed URL 或 fixture 资源地址 |
| `news_ingestion_source` | `enabled` | 停用来源不能手动触发 |
| `news_ingestion_source` | `rate_limit_per_hour` | 手动触发也要遵守的基础限流 |
| `news_ingestion_source` | `default_category_code` | 构造 `IngestionRequest.market/defaultCategory` 的默认来源信息 |
| `news_ingestion_job` | `trigger_type` | 手动触发写入 `MANUAL` |
| `news_ingestion_job` | `job_status` | `RUNNING`、`SUCCESS`、`FAILED` |
| `news_ingestion_job` | `fetched_count/new_count/duplicate_count/candidate_count` | 采集和候选生成统计 |
| `news_ingestion_job` | `error_message` | Provider 或入库失败原因 |

权限模型：

1. 新接口继续使用 Admin Token 鉴权。
2. 只有 Admin 后台可以触发真实 Provider。
3. 用户端 API 不暴露原始采集结果。
4. 候选内容仍需 Admin 审核发布后才能进入用户端。

## 后端实现方案

### API 设计

新增接口：

```http
POST /api/admin/ingestion/sources/{id}/run
Authorization: Bearer <admin-token>
Content-Type: application/json

{
  "pageSize": 5,
  "generateCandidates": true
}
```

响应：

```json
{
  "code": "OK",
  "data": {
    "jobId": 123,
    "sourceCode": "rss-world",
    "providerType": "RSS",
    "status": "SUCCESS",
    "fetchedCount": 5,
    "newCount": 3,
    "duplicateCount": 2,
    "candidateCount": 3,
    "errorMessage": null
  }
}
```

约束：

1. `pageSize` 默认 5，最大 20。
2. `generateCandidates` 默认 true。
3. 来源不存在返回 404。
4. 来源停用返回 422，不创建任务日志。
5. Provider 类型未注册返回 422，并创建失败任务日志。
6. Provider 执行或解析失败时返回 502 或 500，并创建失败任务日志。
7. 触发过于频繁时返回 429 或 422，优先采用 422 以保持当前 Admin API 风格一致。

### 编排服务

新增 `NewsIngestionRunService`，负责手动运行生命周期：

```text
AdminIngestionController
-> AdminIngestionApplicationService.runSource(sourceId, request)
-> NewsIngestionRunService.runManual(source, pageSize, generateCandidates)
   1. 校验 source enabled / baseUrl / rate limit
   2. 创建 RUNNING news_ingestion_job
   3. 按 providerType 找到 NewsIngestionProvider
   4. 构造 IngestionRequest
   5. provider.fetch(source, request)
   6. RawNewsIngestionService 写 raw_news_item 并返回 fetched/new/duplicate
   7. CandidateArticleGenerationService.generatePendingCandidates(sourceCode, limit)
   8. complete job
   9. 返回 AdminIngestionRunResponse
   10. 任一步异常时 fail job 并返回可读错误
```

关键点：

1. 任务日志必须在 Provider 网络请求之前创建，保证外网失败也可观测。
2. 原始资讯入库仍复用 `RawNewsIngestionService` 的去重与最新窗口规则。
3. 候选生成仍复用 `CandidateArticleGenerationService`。
4. 编排服务不写 RSS 特判，只通过 Provider Registry 路由。

### Provider 调用边界

为避免阶段 23 的 RSS Provider 长期依赖环境变量，本阶段扩展 Provider 合约：

```java
public interface NewsIngestionProvider {
    String providerType();

    List<RawNewsPayload> fetch(IngestionRequest request);

    default List<RawNewsPayload> fetch(NewsIngestionSource source, IngestionRequest request) {
        return fetch(request);
    }
}
```

实现策略：

1. `FixtureNewsIngestionProvider` 可继续使用旧 `fetch(request)`。
2. `RssNewsIngestionProvider` 覆盖 `fetch(source, request)`，读取 `source.getBaseUrl()`。
3. `fetch(request)` 保留给 fixture 测试和本地 live smoke，不作为 Admin 手动触发主路径。
4. 后续 API Provider 可以同样覆盖 `fetch(source, request)` 读取 baseUrl/API 配置。

### Job 生命周期

当前 `RawNewsIngestionService.ingest()` 会自己创建并 complete job。阶段 24 为了支持 Provider 拉取失败日志，需要调整职责：

1. 保留现有 `ingest(sourceCode, triggerType, payloads)`，兼容旧测试。
2. 新增一个不创建 job 的入库方法，例如 `ingestPayloads(sourceCode, payloads)`，只返回 fetched/new/duplicate。
3. `NewsIngestionRunService` 自己创建 job、调用 `ingestPayloads()`、生成候选后 complete/fail job。
4. `NewsIngestionJob.complete()` 继续记录 fetched/new/duplicate/candidate。

这样既不破坏已有调用，又能让手动触发拥有完整失败日志。

### 限流与安全

阶段 24 只做基础限流：

1. 按 `source.rateLimitPerHour` 检查最近一小时该 source 的任务数。
2. 超出限制时拒绝本次手动触发。
3. 单次 pageSize 最大 20。
4. 只允许触发已启用且数据库已配置的 source。
5. 不允许前端传 URL。
6. RSS Provider 不保存全文和 PDF。

## 前端影响

Admin 采集任务页增加单源操作：

1. 每张采集源卡片保留启停按钮。
2. 新增“手动采集”按钮，使用 `RadioTower` 或 `RefreshCw` 图标。
3. 运行中只禁用当前 source 的按钮，不锁全页。
4. 成功后刷新 jobs、metrics、sources、anomalies。
5. 失败后在页面底部显示错误，同时任务列表出现 FAILED job 和 errorMessage。
6. mock 模式下模拟新增一条 `SUCCESS` 或 `FAILED` 任务，保持本机无后端也可演示。

不新增独立页面，不改变导航结构。

## 测试与回归方案

后端新增测试：

1. `POST /api/admin/ingestion/sources/{id}/run` 可触发启用的 fixture source。
2. 成功触发后返回 jobId、fetched/new/duplicate/candidate 统计。
3. 停用 source 返回 422。
4. source 不存在返回 404。
5. Provider 抛异常时写入 FAILED job，并返回错误信息。
6. RSS source 使用 `base_url` 调用 Provider，不依赖真实外网。
7. 限流超出时拒绝触发。

Admin 新增测试：

1. API client 映射手动触发响应。
2. mock client 可模拟手动触发并刷新任务列表。
3. Ingestion 页面展示手动采集按钮。
4. 点击按钮后出现 loading/disabled 状态。
5. 成功后刷新任务列表。
6. 失败时展示错误提示。

回归命令：

```powershell
cd backend
.\mvnw.cmd test

cd ..\admin
npm test -- --run
npm run lint
npm run build
```

可选本地真实 smoke：

```powershell
# 前提：本地开发库中存在启用的 RSS source，base_url 为公开 RSS URL。
cd backend
.\mvnw.cmd test
# 启动后端和 Admin，在 Admin 页面点击该 source 的“手动采集”
```

CI 仍不访问真实外网，真实 RSS 只作为本地手动验证。

## 风险与分阶段落地建议

风险：

1. Provider 网络失败可能导致 Admin 操作等待较久，需要短超时和清晰错误信息。
2. 同一 source 连续点击可能产生重复任务，需要按钮 loading 和后端限流。
3. 如果 job 生命周期改动过大，可能影响既有入库测试。
4. RSS source `base_url` 配置错误时，必须失败可见，而不是静默返回空。
5. 候选生成放在同步链路里会增加响应时间；阶段 24 可接受，后续真实来源增多后再异步化。

分阶段落地：

1. 第一批：后端手动触发接口、编排服务、Provider source 配置接入和测试。
2. 第二批：Admin 页面按钮、API client、mock 和页面测试。
3. 第三批：本地真实 RSS source 手动点击 smoke，记录 fetched/new/duplicate/candidate。
4. 后续阶段：定时调度、队列、重试、告警、任务取消和批量触发。

## 当前决策

1. 阶段 24 先做 Admin 单源手动触发，不做定时调度。
2. 手动触发必须使用数据库采集源配置，不允许 Admin 直接输入 URL。
3. 失败日志必须覆盖 Provider fetch/parse 阶段。
4. RSS Provider 正式接入 `news_ingestion_source.base_url`。
5. CI 继续使用 fixture 和 mock，不请求真实 RSS 外网。
6. 全文、PDF、AI 摘要和复杂分类继续保持后续独立阶段。
