# 真实 RSS Provider V1 设计

## 背景与现状

PulseBrief 已完成真实资讯采集基础链路：`NewsIngestionProvider` 抽象、`RawNewsPayload` 统一载荷、原始资讯入库、去重、候选生成、Admin 审核发布、运营监控、Provider 环境配置检查和 CI 验证流水线。当前真实外部 Provider 尚未接入，已有 `FIXTURE` Provider 只用于验证链路，不请求真实外网。

阶段 23 的目标是实现第一个真实 RSS Provider。考虑到后续明确需要全文抓取、授权 PDF、AI 摘要和复杂分类，本阶段不能把 RSS 做成临时脚本，也不能把 RSS 解析、全文抓取、PDF 下载、摘要和分类全部塞进同一个 Provider。正确方向是：RSS Provider 作为真实采集的第一层入口，只负责把 RSS 条目规范化为 `RawNewsPayload`；后续内容增强能力作为独立阶段接入。

现有相关设计边界：

1. `docs/真实Provider环境配置与密钥检查设计.md` 已要求真实采集默认关闭、RSS URL 不允许使用占位符、禁止历史回溯、控制最新窗口和限流。
2. `docs/最新内容与授权全文PDF采集V1设计.md` 已明确全文和 PDF 必须来源授权明确，且必须遵守最新内容窗口和 Admin 审核。
3. `RawNewsIngestionService` 当前只消费 `RawNewsPayload`，不关心 Provider 类型，适合继续保持 Provider 与入库链路隔离。

## 目标与非目标

目标：

1. 实现第一个真实 RSS Provider 的设计边界。
2. 本阶段只采集 RSS 元数据：标题、摘要、来源、发布时间、原文链接、可选图片、语言、国家和原始 payload。
3. 保持 `NewsIngestionProvider -> RawNewsPayload -> RawNewsIngestionService` 主链路稳定。
4. 明确 RSS Provider 不负责全文抓取、PDF 下载、AI 摘要和复杂分类。
5. 为后续全文、PDF、AI 摘要和复杂分类预留独立处理阶段，避免阶段 23 埋技术债。
6. 明确测试策略：自动化测试使用本地 RSS fixture，不让 CI 请求真实外网；阶段 23 交付前必须执行一次本地手动 live smoke，验证真实 RSS 外网采集能拉取并入库开发库数据。

非目标：

1. 本阶段不抓取网页全文。
2. 本阶段不下载、缓存或解析 PDF。
3. 本阶段不接入 AI 摘要服务。
4. 本阶段不实现复杂分类模型或推荐算法。
5. 本阶段不做历史回溯、翻页抓取或归档同步。
6. 本阶段不新增 Admin 手动触发 UI；手动触发和失败日志闭环放到后续阶段。
7. 本阶段不修改 Flutter 用户端展示逻辑。

## 影响范围

后端：

1. 新增 RSS Provider 实现，用于解析 RSS/Atom feed 并输出 `RawNewsPayload`。
2. 新增 RSS fixture 测试资源和 Provider 单元测试。
3. 必要时补充 Provider 上下文测试，确认 `RSS` Provider 能被 Spring 管理。
4. 不修改用户端 API、不修改 Admin 发布 API、不修改 Flutter 读取逻辑。

配置：

1. 沿用 `PULSEBRIEF_INGESTION_ENABLED` 默认关闭策略。
2. 沿用 `PULSEBRIEF_PROVIDER_KIND=RSS` 和 `PULSEBRIEF_RSS_FEED_URLS` 配置边界。
3. 沿用 `PULSEBRIEF_INGESTION_MAX_AGE_HOURS`、`PULSEBRIEF_INGESTION_MAX_ITEMS_PER_SOURCE` 和 `PULSEBRIEF_PROVIDER_RATE_LIMIT_PER_HOUR` 的安全限制。
4. 本地 live smoke 额外使用显式开关，例如 `PULSEBRIEF_RSS_LIVE_TEST_ENABLED=true`，避免误把真实外网测试放进默认流程。

文档：

1. 新增本设计文档。
2. 后续实现完成后再更新任务清单、README 和测试方案。

## 数据模型或权限模型

本阶段不新增数据库表，不扩展现有表字段。

RSS 元数据映射到现有 `RawNewsPayload`：

| RSS/Atom 字段 | `RawNewsPayload` 字段 | 说明 |
| --- | --- | --- |
| `guid` / `id` / `link` | `providerItemId` | 优先使用稳定 ID；缺失时使用链接 |
| `title` | `title` | 必填，空标题条目跳过 |
| `description` / `summary` | `summary` | 只保存 RSS 提供的摘要或描述，不抓网页全文 |
| feed title / entry source | `sourceName` | 优先使用条目来源，缺失时使用 feed 标题 |
| `link` | `originalUrl` | 必填，空链接条目跳过 |
| media enclosure / media thumbnail | `imageUrl` | 可为空，只保存图片 URL |
| `published` / `updated` / `pubDate` | `publishedAt` | 优先发布时间，其次更新时间；缺失则允许入库但不进入全文/PDF阶段 |
| feed language | `language` | 可为空，默认由采集源或后续规则补齐 |
| feed country / source config | `country` | 可为空，默认由采集源或后续规则补齐 |
| 原始条目快照 | `rawPayload` | 保存必要 JSON 快照，避免保存网页全文 |

权限模型不变：

1. RSS Provider 运行在后端采集链路内，不对用户端暴露。
2. 用户端只读取已发布内容，不访问 `raw_news_item`。
3. Admin 审核发布仍使用现有 Admin 权限边界。

## 后端实现方案

### 架构原则

阶段 23 采用“轻量入口 + 可扩展流水线”的方案：

```text
RSS Provider 拉取/解析 feed
-> RawNewsPayload
-> RawNewsIngestionService 入库/去重/最新窗口过滤
-> CandidateArticleGenerationService 生成候选
-> Admin 审核发布

后续独立阶段：
-> ContentFetchService 授权正文片段/全文
-> ReportAssetService 授权 PDF 元数据/缓存
-> AiSummaryService AI 摘要
-> ClassificationService 规则/模型分类
```

关键约束：

1. `RssNewsIngestionProvider` 只做 RSS/Atom 解析和字段规范化。
2. 不在 `RawNewsIngestionService` 中写 RSS 特判。
3. 不把 RSS 特有字段直接扩散到 `raw_news_item` 或候选发布模型。
4. 后续全文、PDF、AI 摘要和复杂分类通过独立服务读取已入库内容或候选内容，不反向污染 Provider。

### RSS Provider

建议新增：

```text
backend/src/main/java/com/pulsebrief/ingestion/provider/RssNewsIngestionProvider.java
backend/src/test/java/com/pulsebrief/ingestion/provider/RssNewsIngestionProviderTest.java
backend/src/test/resources/ingestion/rss/sample-feed.xml
```

Provider 行为：

1. `providerType()` 返回 `RSS`。
2. `fetch(IngestionRequest request)` 读取配置或请求中的 RSS URL。
3. 支持 RSS 2.0 和 Atom 常见字段。
4. 按 `request.pageSizeOrDefault()` 限制返回条数。
5. 对标题、摘要、来源、链接做 trim 和空值保护。
6. 对发布时间做容错解析；无法解析时保留 `publishedAt=null`，交由后续质量检测和最新窗口规则处理。
7. 对 RSS 解析失败抛出可观测异常，由采集任务日志记录失败原因。

### RSS 解析依赖

优先选择成熟 Java RSS/Atom 解析库，而不是手写 XML 字符串解析。候选方案：

1. 使用 Rome：成熟、覆盖 RSS/Atom 常见格式，适合本阶段。
2. 使用 JDK XML + 自己映射：依赖少，但格式兼容成本高。
3. 使用 HTTP 客户端 + XML DOM 手写解析：只适合临时验证，不建议作为长期方案。

推荐使用 Rome，并在设计文档批准后通过实现计划确认具体依赖版本和许可证。

### 网络访问边界

本阶段真实运行可访问配置的 RSS URL。阶段 23 交付前需要做一次本地手动 live smoke，确认真实 RSS 外网采集可以拉到数据并进入开发库；CI 和自动化测试仍不得访问真实外网。

规则：

1. CI 保持 `PULSEBRIEF_INGESTION_ENABLED=false`。
2. Provider 单元测试读取本地 fixture XML。
3. live smoke 只通过本地手动命令运行，并使用 `.env.local` 或本地数据库采集源配置。
4. live smoke 必须显式开启 `PULSEBRIEF_RSS_LIVE_TEST_ENABLED=true`。
5. live smoke 单源单次最多拉取 5 条，避免对外部来源造成压力。
6. live smoke 只允许使用公开、无需登录、无需密钥、许可边界清晰的 RSS URL。
7. live smoke 的输出需要记录采集源、拉取条数、新增条数、重复条数和候选生成情况。
8. 不在自动化测试中依赖某个真实 RSS 源的当前内容、发布时间或网络稳定性。

### 阶段 23 live smoke 验证

阶段 23 实现完成后，需要在本地开发环境执行真实外网采集验证。该验证不作为 CI 阻断条件，但作为阶段 23 交付检查项。

建议流程：

```powershell
.\scripts\check-provider-env.ps1 -EnvFile .\.env.local
cd backend
.\mvnw.cmd -Dtest=RssNewsIngestionProviderTest test
# 手动启动一次 RSS live smoke，具体命令在实现计划中确定
```

验收标准：

1. live smoke 使用真实公开 RSS URL，网络请求成功。
2. Provider 至少解析到 1 条有效 RSS 条目。
3. 入库流程返回 `fetchedCount >= 1`。
4. 若内容已存在，允许 `newCount=0` 且 `duplicateCount >= 1`。
5. 采集任务日志记录本次 source code、trigger type、成功状态和计数。
6. 数据仍只包含 RSS 元数据，不保存网页全文或 PDF 文件。
7. 测试完成后不提交 `.env.local`、真实采集输出文件、PDF 或本地数据库 dump。

## 后续独立处理阶段

### 全文抓取阶段

后续新增 `ContentFetchService`，只处理已经入库的 `raw_news_item` 或候选内容。

边界：

1. 仅当 `content_access_policy` 为 `SNIPPET_ALLOWED` 或 `FULLTEXT_ALLOWED` 时运行。
2. 不绕过登录、付费墙、验证码、robots 或反爬限制。
3. 保存正文片段或授权全文时，写入后续内容字段，不改变 RSS Provider 输出模型。
4. 失败只记录内容增强失败，不影响原始 RSS 元数据入库。

### PDF 阶段

后续继续沿用 `report_asset` 思路，新增或扩展 `ReportAssetService`。

边界：

1. 仅当来源许可为 `PDF_ALLOWED` 且明确授权下载或缓存时运行。
2. PDF 必须有最新窗口、大小限制、文件哈希、来源 URL 和授权说明。
3. PDF 文件不提交 Git，先存本地数据目录或 Docker volume，后续可迁移对象存储。
4. PDF 解析和摘要不属于阶段 23。

### AI 摘要阶段

后续新增 `AiSummaryService` 或异步摘要任务。

边界：

1. 输入来自已审核或可审核的标题、摘要、授权正文片段或授权全文。
2. 输出作为候选摘要或结构化要点，需要 Admin 审核后才能发布。
3. AI 服务密钥不进入 Git，不进入 CI 默认流程。
4. AI 摘要失败不影响 RSS 元数据采集。

### 复杂分类阶段

后续新增 `ClassificationService`。

分阶段策略：

1. 第一层使用规则分类：来源默认分类、关键词、标题/摘要规则。
2. 第二层可引入模型分类或 AI 分类。
3. 分类结果写入候选内容或分类建议，不让 RSS Provider 直接决定最终分类。
4. Admin 可覆盖分类，用户端只读取已发布分类。

## 前端影响

Flutter 用户端：

1. 阶段 23 无 UI 和接口改动。
2. RSS 入库后仍需经过候选审核发布，用户端才会看到真实内容。
3. 后续授权全文、PDF、AI 摘要和复杂分类上线时，再分别设计详情页展示和入口。

React Admin：

1. 阶段 23 不新增页面。
2. 现有采集任务和异常检测可继续观察 RSS 元数据质量。
3. 阶段 24 再补 Admin 手动触发真实 Provider、失败日志查看和单源开关闭环。

## 测试与回归方案

阶段 23 新增测试：

1. RSS fixture 能解析为 `RawNewsPayload`。
2. Atom fixture 能解析为 `RawNewsPayload`。
3. 缺少标题或链接的条目被跳过。
4. `guid` 缺失时使用链接作为 `providerItemId`。
5. 发布时间解析失败时不会抛出整体失败，条目 `publishedAt` 为 `null`。
6. 关键词过滤和 page size 限制与 fixture Provider 保持一致。
7. Spring 上下文中同时存在 `FIXTURE` 和 `RSS` Provider。
8. 本地 live smoke 使用真实公开 RSS URL 验证外网采集链路，结果记录在阶段 23 交付说明中，不进入 CI。

阶段 23 回归命令：

```powershell
cd backend
.\mvnw.cmd -Dtest=RssNewsIngestionProviderTest test
.\mvnw.cmd test
cd ..
.\scripts\check-provider-env.ps1
.\scripts\check-provider-env.ps1 -EnvFile .\.env.example
```

完整发布前回归继续沿用现有 CI：

```powershell
cd backend
.\mvnw.cmd test
cd ..\admin
npm test -- --run
npm run lint
npm run build
cd ..\mobile
flutter analyze
flutter test
cd ..
docker compose -f deploy/docker-compose.yml config
```

CI 规则：

1. CI 不请求真实 RSS。
2. CI 不要求外部 RSS URL、AI Key 或 PDF 存储配置。
3. Provider 测试只使用本地 fixture。
4. live smoke 失败时先按外部网络、RSS 源可用性、解析兼容性和入库链路分层定位，不把外部源波动误判为 CI 失败。

## 风险与分阶段落地建议

风险：

1. RSS 源格式差异较大，必须使用成熟解析库和 fixture 覆盖常见格式。
2. RSS 摘要可能包含 HTML，需要清理标签和限制长度。
3. RSS 缺失发布时间时，最新窗口过滤能力变弱，需要质量检测兜底。
4. 真实外网源不稳定，不能让 CI 或默认开发环境依赖外部请求。
5. 后续全文、PDF、AI 摘要和分类如果混入 Provider，会导致 Provider 变成难维护的“大采集器”。
6. 来源许可和版权风险仍需人工确认，不能因为 RSS 可访问就默认允许全文或 PDF。

分阶段建议：

1. 阶段 23：实现 RSS 元数据 Provider，仅产出 `RawNewsPayload`。
2. 阶段 23 交付前：执行本地手动 live smoke，验证真实 RSS 外网采集数据可以进入开发库。
3. 阶段 24：增加 Admin 手动触发、真实 Provider 失败日志和单源开关闭环。
4. 阶段 25：实现授权正文片段/全文抓取服务。
5. 阶段 26：实现授权 PDF 元数据入库和可选缓存。
6. 阶段 27：实现 AI 摘要任务和 Admin 审核入口。
7. 阶段 28：实现复杂分类服务，并允许 Admin 覆盖分类结果。

## 当前决策

1. 阶段 23 采用方案 1 的升级版：先落地真实 RSS 元数据 Provider，同时明确后续内容增强流水线边界。
2. RSS Provider 只负责 RSS/Atom 解析和 `RawNewsPayload` 映射。
3. 全文抓取、PDF、AI 摘要和复杂分类都作为后续独立服务，不进入 RSS Provider。
4. 阶段 23 必须包含一次本地手动真实 RSS 外网采集验证，证明能拉取并入库开发库数据。
5. CI 和自动化测试不访问真实 RSS 外网。
6. 不新增数据库结构，避免在第一轮 RSS 接入中扩大改动面。
7. 设计批准后，再进入实现计划和编码。

## 阶段 23 实现记录

实现完成后保留以下交付记录，作为后续阶段 24 接入 Admin 手动触发和单源配置时的基线：

1. 自动化测试：`RssNewsIngestionProviderTest` 使用本地 RSS/Atom fixture 覆盖元数据映射、关键词过滤、page size 限制、缺失字段跳过和无效发布时间容错。
2. Spring 上下文：`NewsIngestionProviderContextTest` 确认 `FIXTURE` 和 `RSS` Provider 均已注册。
3. 外网 smoke：`RssNewsIngestionProviderLiveTest` 默认跳过，只有 `PULSEBRIEF_RSS_LIVE_TEST_ENABLED=true` 时才请求真实 RSS。
4. 本机验证：2026-06-15 使用 `https://rss.nytimes.com/services/xml/rss/nyt/World.xml` 执行 live smoke 通过，成功拉取并解析真实 RSS 元数据。
5. 网络边界：本机 Java 请求需要读取 `HTTP_PROXY` / `HTTPS_PROXY`；该能力只影响 RSS HTTP 客户端，不改变 CI 默认行为。
6. 范围确认：阶段 23 仍未实现网页全文、PDF 下载/解析、AI 摘要或复杂分类，这些能力继续作为后续独立阶段推进。
