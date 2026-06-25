# 授权 PDF 元数据与文件缓存设计

## 背景与现状

PulseBrief 已完成真实 RSS 元数据采集、候选资讯生成、Admin 审核发布、阶段 25 授权正文片段抓取。PDF 能力此前只完成了基础元数据登记：`report_asset` 可记录候选资讯关联的 PDF 标题、原始链接、文件名、文件大小、文件哈希、授权策略和审核状态。

现有基础：

1. `news_ingestion_source` 已有 `content_access_policy`、`allow_pdf_download`、`max_age_hours` 和 `license_note`。
2. `report_asset` 已存在，当前用于登记授权 PDF 元数据。
3. `ReportAssetRegistrationService` 已支持 `PDF_ALLOWED` 元数据登记和 `file_hash` 去重。
4. Admin 候选详情已返回 `reportAssets`，候选发布前会阻止未 `APPROVED` 的 PDF 资产发布。
5. 当前还没有真实 PDF 下载、缓存路径、MIME 校验、下载状态、下载错误、文件存储清理和 Admin PDF 合规操作闭环。

阶段 26 的核心目标是把“授权 PDF 元数据”和“本地缓存文件”设计成独立、可审计、可回退的能力。PDF 文件本身不进入 Git，不进入 CI artifact，也不绕过来源访问控制。

## 目标与非目标

目标：

1. 设计授权 PDF 下载许可校验：仅 `PDF_ALLOWED` 且 `allow_pdf_download=true` 且有授权说明的 source 可下载。
2. 复核现有 `report_asset` 模型，明确需要补充的缓存状态、存储路径、MIME、错误信息和审核字段。
3. 设计 PDF 文件缓存目录、命名规则、大小限制、哈希去重、临时文件和清理策略。
4. 设计 Admin PDF 资产预览、缓存触发、合规审批、拒绝和发布前检查。
5. 设计自动化测试和真实公开 PDF 手动 smoke 策略。
6. 保持 PDF、正文、AI 摘要和复杂分类相互独立，后续可进入异步队列。

非目标：

1. 不下载付费研报、登录后 PDF、券商内部研报、数据库供应商内容或需要 Cookie/Token/验证码的文件。
2. 不批量回溯历史 PDF，不抓取历史研报库。
3. 不做 OCR、PDF 正文解析、表格抽取或 AI 摘要；这些属于后续独立阶段。
4. 不把未审核 PDF 暴露给 Flutter 用户端。
5. 不在 CI 访问真实外网或保存真实 PDF 文件。
6. 不引入对象存储强依赖；V1 先支持本地目录或 Docker volume，后续可迁移到 S3/OSS/MinIO。

## 影响范围

后端：

1. 新增或扩展 PDF 缓存相关表结构。
2. 新增 PDF 下载客户端、许可策略校验、文件存储服务和缓存编排服务。
3. 扩展 Admin 候选详情中的 PDF 资产字段。
4. 新增 Admin PDF 缓存、审批和拒绝接口。
5. 候选发布前继续校验 PDF 资产合规状态。

Admin：

1. 候选详情的 PDF 资产区块展示下载状态、文件大小、MIME、缓存路径摘要、许可策略和授权说明。
2. 授权 source 可手动触发 PDF 缓存。
3. 运营可审批、拒绝或保留原文链接。
4. 缓存失败时展示失败原因，不阻断候选按摘要和原文链接继续审核。

Flutter：

1. 阶段 26 默认不新增用户端 PDF 展示。
2. 后续若开放 PDF 入口，用户端只能读取已发布且已审批的 PDF 资产元数据或下载接口。

部署与配置：

1. PDF 缓存目录使用本地 `data/reports` 或 Docker volume。
2. 默认关闭真实 PDF 下载 smoke。
3. PDF 缓存目录、大小上限、超时和开关通过环境变量配置。

## 数据模型或权限模型

### 下载许可

PDF 下载前必须同时满足：

1. `news_ingestion_source.enabled = true`。
2. `news_ingestion_source.content_access_policy = 'PDF_ALLOWED'`。
3. `news_ingestion_source.allow_pdf_download = 1`。
4. `news_ingestion_source.license_note` 非空，且能说明授权来源。
5. 候选关联的 `raw_news_item.published_at` 在 `max_age_hours` 最新窗口内。
6. PDF URL 是公开 `http` 或 `https`，不需要登录、Cookie、Token、验证码或绕过访问控制。
7. 响应 MIME 为 `application/pdf`，或来源明确且文件扩展名为 `.pdf`。

未满足条件时：

1. 不下载文件。
2. 不写入本地缓存。
3. Admin 展示不可缓存原因。
4. 候选仍可按摘要和原文链接继续审核。

### 现有模型复核

当前 `report_asset` 同时承担“候选 PDF 资产”和“物理文件唯一身份”两类职责：

1. `candidate_article_id` 表明它是候选级资产。
2. `file_hash` 又有唯一约束，表明它是物理文件级身份。

这会带来一个后续问题：同一 PDF 被多个候选引用时，`uk_report_asset_file_hash` 会让第二个候选无法拥有自己的资产审核状态。阶段 26 推荐拆分逻辑资产和物理缓存文件。

### 推荐模型

保留 `report_asset` 作为候选级 PDF 资产表，新增 `report_asset_file` 作为物理缓存文件表。

`report_asset` 建议扩展：

| 字段 | 说明 |
| --- | --- |
| `asset_file_id` | 关联物理缓存文件，缓存成功后填充 |
| `license_note` | PDF 授权说明快照 |
| `cache_status` | `NOT_CACHED/PENDING/SUCCESS/FAILED/SKIPPED` |
| `cache_error_message` | 缓存失败或跳过原因 |
| `cache_requested_at` | 缓存请求时间 |
| `cache_completed_at` | 缓存完成时间 |
| `review_note` | Admin PDF 合规审核备注 |
| `reviewed_at` | Admin 审核时间 |
| `reviewed_by` | Admin 操作人，开发态可先固定为 `dev-admin` |

`report_asset_file` 建议新增：

| 字段 | 说明 |
| --- | --- |
| `id` | 主键 |
| `file_hash` | SHA-256 文件哈希，唯一 |
| `storage_provider` | `LOCAL`，后续可扩展 `S3/OSS/MINIO` |
| `storage_path` | 相对存储路径，如 `reports/source/yyyyMMdd/hash.pdf` |
| `file_name` | 规范化文件名 |
| `file_size_bytes` | 文件大小 |
| `mime_type` | MIME 类型 |
| `downloaded_at` | 下载完成时间 |
| `created_at/updated_at` | 审计时间 |

兼容策略：

1. 短期保留 `report_asset.file_hash`、`file_size_bytes` 和 `downloaded_at` 字段，避免一次性大迁移。
2. 新实现优先使用 `report_asset.asset_file_id -> report_asset_file`。
3. 后续稳定后再评估是否移除或废弃 `report_asset.file_hash` 唯一约束。

### 状态机

PDF 缓存状态：

```text
NOT_CACHED
-> PENDING
-> SUCCESS
-> FAILED
-> SKIPPED
```

PDF 审核状态继续使用 `asset_status`：

```text
PENDING_REVIEW
-> APPROVED
-> REJECTED
-> PUBLISHED
```

发布规则：

1. 有 `PENDING_REVIEW`、`REJECTED` 或缓存失败的 PDF 资产时，默认不发布 PDF 入口。
2. 候选若仍要发布文章摘要，可由 Admin 选择“仅保留原文链接”并拒绝 PDF 资产。
3. 只有 `asset_status = APPROVED` 且 `cache_status = SUCCESS` 的资产，后续才允许进入用户端 PDF 入口。

## 后端实现方案

### 服务拆分

建议新增：

```text
PdfAssetPolicyService
PdfDownloadClient
PdfStorageService
PdfAssetCacheService
ReportAssetFileRepository
```

职责：

1. `PdfAssetPolicyService`：校验 source、候选、最新窗口、授权说明和 `allow_pdf_download`。
2. `PdfDownloadClient`：只做公开 PDF 下载，负责超时、大小限制、MIME 校验和流式读取。
3. `PdfStorageService`：负责临时文件、SHA-256、原子移动、路径规范化和删除临时文件。
4. `PdfAssetCacheService`：编排许可校验、下载、哈希去重、文件记录、资产状态更新和失败记录。
5. `ReportAssetFileRepository`：按 `file_hash` 查找或保存物理文件记录。

### 配置建议

```text
PULSEBRIEF_PDF_CACHE_ENABLED=false
PULSEBRIEF_PDF_STORAGE_DIR=./data/reports
PULSEBRIEF_PDF_MAX_SIZE_MB=25
PULSEBRIEF_PDF_TIMEOUT_SECONDS=15
PULSEBRIEF_PDF_LIVE_TEST_ENABLED=false
```

默认行为：

1. 自动化测试不需要开启真实下载。
2. Admin 手动缓存可以在本地开发环境开启。
3. CI 不开启 `PULSEBRIEF_PDF_LIVE_TEST_ENABLED`。
4. Docker 部署时挂载 `data/reports` volume，不把 PDF 放入镜像。

### 存储规则

本地路径建议：

```text
data/reports/{sourceCode}/{yyyyMMdd}/{sha256}.pdf
```

规则：

1. 文件名只用于展示，不直接作为存储路径的一部分。
2. 先写入 `data/reports/tmp/{uuid}.download`。
3. 下载完成后计算 SHA-256。
4. 如果哈希已存在，删除临时文件并复用已有 `report_asset_file`。
5. 如果哈希不存在，原子移动到最终路径。
6. 超过大小限制、MIME 不符、下载失败或中断时删除临时文件。

### API 设计

候选详情 PDF 资产响应扩展：

```json
{
  "id": 21,
  "title": "AI infrastructure outlook",
  "originalUrl": "https://example.com/report.pdf",
  "fileName": "ai-infrastructure-outlook.pdf",
  "fileSizeBytes": 1024,
  "mimeType": "application/pdf",
  "licensePolicy": "PDF_ALLOWED",
  "licenseNote": "来源授权说明",
  "status": "PENDING_REVIEW",
  "cacheStatus": "SUCCESS",
  "cacheErrorMessage": null,
  "cachedAt": "2026-06-25T10:00:00"
}
```

手动缓存：

```http
POST /api/admin/candidates/{candidateId}/report-assets/{assetId}/cache
Authorization: Bearer <admin-token>
```

审批：

```http
POST /api/admin/candidates/{candidateId}/report-assets/{assetId}/approve
Authorization: Bearer <admin-token>

{
  "reviewNote": "已确认来源公开授权"
}
```

拒绝：

```http
POST /api/admin/candidates/{candidateId}/report-assets/{assetId}/reject
Authorization: Bearer <admin-token>

{
  "reviewNote": "来源未明确允许缓存"
}
```

接口约束：

1. 候选不存在返回 404。
2. PDF 资产不属于候选返回 404。
3. 候选非 `PENDING_REVIEW` 返回 409。
4. source 未授权或未开启 `allow_pdf_download` 返回 `200 + SKIPPED` 或 422；推荐 `200 + SKIPPED` 便于 Admin 展示原因。
5. 下载失败返回 `200 + FAILED`，不阻断候选继续审核。

### 与阶段 25 的关系

PDF 缓存服务不复用 `ContentFetchService`：

1. 正文抓取处理 HTML。
2. PDF 缓存处理二进制文件、MIME、文件哈希和存储路径。
3. 两者都读取 source 许可策略，但状态、输出和风险不同。
4. 后续 AI 摘要只能读取已审核且授权允许的输入，不直接读取未审核 PDF。

## 前端影响

Admin 候选详情 PDF 资产区块需要展示：

1. PDF 标题、文件名、来源链接。
2. 授权策略和授权说明。
3. 缓存状态：未缓存、缓存中、成功、失败、跳过。
4. 文件大小、MIME 和缓存时间。
5. 失败原因。
6. 操作按钮：缓存 PDF、审批 PDF、拒绝 PDF、打开原始链接。

交互规则：

1. 未授权 source 不展示“缓存 PDF”按钮，只展示原因。
2. 缓存失败后可重新触发，但必须限流。
3. 审批前必须能看到授权说明和文件元数据。
4. 拒绝后候选仍可按摘要和原文链接发布。
5. 发布按钮若被 PDF 合规状态阻断，页面需要展示明确原因。

Flutter 用户端阶段 26 默认不改动。后续开放时：

1. 只展示已发布文章关联的 `APPROVED + SUCCESS` PDF。
2. 默认展示“查看公开 PDF”或“下载公开报告”入口。
3. 不展示未审核、失败、拒绝或未授权 PDF。

## 测试与回归方案

后端自动化测试使用本地 fixture PDF 或 fake downloader，不访问真实外网：

1. `PDF_ALLOWED + allow_pdf_download=true + license_note` 可缓存 fixture PDF。
2. `PDF_ALLOWED` 但 `allow_pdf_download=false` 返回 `SKIPPED`，不写文件。
3. `SUMMARY_ONLY/LINK_ONLY/UNKNOWN` source 不缓存 PDF。
4. 缺少 `license_note` 时不缓存 PDF。
5. 超出 `max_age_hours` 的候选不缓存 PDF。
6. 非 PDF MIME 返回 `FAILED` 或 `SKIPPED`，不保留文件。
7. 超过大小限制时删除临时文件并记录失败。
8. 下载中断时删除临时文件并记录失败。
9. 相同文件哈希复用同一 `report_asset_file`。
10. 不同候选可拥有独立 `report_asset` 审核状态，同时复用同一物理文件。
11. 未审批 PDF 资产阻止 PDF 入口发布。
12. 拒绝 PDF 后候选可选择仅保留原文链接发布。

Admin 测试：

1. API client 映射 PDF 缓存状态、MIME、文件大小、授权说明和错误信息。
2. 候选详情展示“缓存 PDF”“审批 PDF”“拒绝 PDF”操作。
3. mock client 支持缓存成功、失败、审批和拒绝状态流转。
4. 发布被 PDF 合规状态阻断时展示原因。

回归命令建议：

```powershell
cd backend
.\mvnw.cmd "-Dtest=PdfAssetCacheServiceTest,AdminCandidateReportAssetControllerTest,IngestionSchemaTest" test
.\mvnw.cmd "-Dtest=PdfAssetCacheLiveSmokeTest" test
.\mvnw.cmd test

cd ..\admin
npm test -- --run adminApi.test.ts App.test.tsx
npm test -- --run
npm run lint
npm run build
```

真实公开 PDF smoke：

1. 必须显式开启 `PULSEBRIEF_PDF_LIVE_TEST_ENABLED=true`。
2. 只允许公开、无需登录、授权明确的官方 PDF。
3. 单次最多缓存 1 个 PDF。
4. smoke 使用临时或本地开发存储目录。
5. smoke 记录 source、candidate、asset、文件大小、哈希、MIME 和状态。
6. 不提交 PDF 文件、缓存目录、数据库 dump、真实网页快照或本地密钥。

阶段 26 真实公开 PDF smoke 记录：

1. 时间：2026-06-25 11:24:49 +08:00。
2. 来源：美国 IRS 官方公开 PDF `https://www.irs.gov/pub/irs-pdf/fw4.pdf`，无需登录，`Content-Type=application/pdf`，`Content-Length=208845`。
3. 命令：

```powershell
cd backend
$env:PULSEBRIEF_PDF_LIVE_TEST_ENABLED='true'
$env:PULSEBRIEF_PDF_LIVE_TEST_URL='https://www.irs.gov/pub/irs-pdf/fw4.pdf'
.\mvnw.cmd "-Dtest=PdfAssetCacheLiveSmokeTest" test
```

4. 结果：`Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`。
5. 后端记录：`source=live-pdf-smoke-c6f362bb-4858-4a09-9407-4ae4059b1986`，`candidateId=324`，`assetId=73`，`fileId=3`，`status=SUCCESS`。
6. 文件记录：`bytes=208845`，`mime=application/pdf`，`fileHash=92444d8856ce55d9e25dca8b6d1420634fc68b11e1ab1f760916ea29ddd312b2`。
7. 缓存目录：`backend/target/live-pdf-cache`，不提交 PDF 文件或缓存目录。

CI 规则：

1. CI 不访问真实 PDF URL。
2. CI 不上传 PDF artifact。
3. CI 不依赖代理、对象存储、AI Key 或外部解析服务。

## 风险与分阶段落地建议

风险：

1. PDF 版权风险高，必须依赖来源授权和 Admin 审核。
2. 文件体积可能导致磁盘占用增长，需要大小限制和清理策略。
3. 相同 PDF 可能出现在多个候选中，必须拆分候选审核状态和物理文件缓存。
4. MIME 与扩展名可能不一致，不能只信文件名。
5. 下载失败常见，不能阻断 RSS 元数据采集和候选生成。
6. 后续 AI 摘要若读取 PDF，必须再次校验授权范围。

分阶段建议：

1. 第一批：只写设计文档、测试方案和任务清单。
2. 第二批：新增 `report_asset_file` 与 `report_asset` 缓存字段 migration。
3. 第三批：实现 fake downloader 驱动的 `PdfAssetCacheService` 和后端测试。
4. 第四批：补 Admin API 与页面状态流转。
5. 第五批：本地显式公开 PDF smoke。
6. 第六批：再评估用户端 PDF 入口，不默认开放。

## 当前决策

1. 阶段 26 采用“候选 PDF 资产”和“物理缓存文件”分离设计。
2. `report_asset` 继续作为候选级资产和合规审核入口。
3. 新增 `report_asset_file` 保存物理文件路径、哈希、大小和 MIME。
4. 仅 `PDF_ALLOWED + allow_pdf_download=true + license_note` 可进入 PDF 缓存。
5. PDF 缓存失败不影响 RSS 元数据入库、候选生成和正文抓取。
6. 阶段 26 不做 PDF 解析、AI 摘要、OCR 或用户端 PDF 入口。
7. 自动化测试只使用 fixture/fake；真实公开 PDF 只允许本地显式 smoke。
