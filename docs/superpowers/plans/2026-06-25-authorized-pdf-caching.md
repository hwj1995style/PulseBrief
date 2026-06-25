# Authorized PDF Caching Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 落地阶段 26 “授权 PDF 元数据与文件缓存”的第一批实现，形成后端缓存、Admin 审批和发布前合规校验闭环。

**Architecture:** 保留 `report_asset` 作为候选级 PDF 资产表，新增 `report_asset_file` 保存物理缓存文件。缓存服务只处理公开授权 PDF 的二进制下载、MIME 校验、哈希去重和本地路径登记，不解析 PDF 文本、不生成摘要、不暴露给 Flutter 用户端。

**Tech Stack:** Spring Boot 3.5、JPA、Flyway、MySQL 8、JUnit/MockMvc、React/Vite/Vitest、TypeScript。

---

## 阶段边界

本批实现：

- 拆分候选级资产和物理缓存文件。
- 新增授权 PDF 缓存服务与本地文件存储。
- 新增 Admin 缓存、审批、拒绝接口。
- 扩展 Admin 候选详情 PDF 区块。
- 保持测试使用 fake downloader 或本地 fixture，不访问真实外网。

本批不做：

- PDF 解析、OCR、表格抽取。
- AI 摘要、复杂分类。
- Flutter 用户端 PDF 入口。
- 登录后、付费、Cookie/Token/验证码 PDF 下载。
- CI 中真实外网 PDF smoke。

## 文件结构

- Create: `backend/src/main/resources/db/migration/V11__report_asset_cache.sql`
  - 新建 `report_asset_file`，扩展 `report_asset` 缓存和审核字段。
- Create: `backend/src/main/java/com/pulsebrief/ingestion/domain/ReportAssetFile.java`
  - 物理缓存文件实体。
- Create: `backend/src/main/java/com/pulsebrief/ingestion/repository/ReportAssetFileRepository.java`
  - 按 `fileHash` 查询物理缓存文件。
- Modify: `backend/src/main/java/com/pulsebrief/ingestion/domain/ReportAsset.java`
  - 新增 `assetFile`、`licenseNote`、`cacheStatus`、错误、缓存时间和审核备注字段。
- Modify: `backend/src/main/java/com/pulsebrief/ingestion/repository/ReportAssetRepository.java`
  - 支持候选内 file hash 去重。
- Modify: `backend/src/main/java/com/pulsebrief/ingestion/service/ReportAssetRegistrationService.java`
  - 保持 PDF 元数据登记只登记候选资产，不直接下载文件。
- Create: `backend/src/main/java/com/pulsebrief/ingestion/service/DownloadedPdf.java`
  - PDF 下载结果 record。
- Create: `backend/src/main/java/com/pulsebrief/ingestion/service/PdfDownloadClient.java`
  - PDF 下载接口，测试用 fake 实现覆盖。
- Create: `backend/src/main/java/com/pulsebrief/ingestion/service/HttpPdfDownloadClient.java`
  - 生产 HTTP 下载实现，校验 URL、MIME 和大小。
- Create: `backend/src/main/java/com/pulsebrief/ingestion/service/PdfAssetCacheService.java`
  - 策略校验、下载、哈希、存储、去重和状态写入。
- Create: `backend/src/main/java/com/pulsebrief/ingestion/config/PdfCacheProperties.java`
  - 读取 PDF 缓存目录、大小、超时和开关。
- Modify: `backend/src/main/resources/application.yml`
  - 增加 `pulsebrief.pdf-cache` 默认配置。
- Create: `backend/src/main/java/com/pulsebrief/admin/api/AdminReportAssetActionRequest.java`
  - PDF 审批/拒绝备注请求。
- Modify: `backend/src/main/java/com/pulsebrief/admin/api/AdminReportAssetResponse.java`
  - 返回缓存状态、错误、MIME、缓存时间和授权说明。
- Modify: `backend/src/main/java/com/pulsebrief/admin/api/AdminCandidateController.java`
  - 新增缓存、审批、拒绝接口。
- Modify: `backend/src/main/java/com/pulsebrief/admin/service/AdminCandidateApplicationService.java`
  - 组合缓存服务和审核状态变更。
- Modify: `backend/src/main/java/com/pulsebrief/admin/service/AdminCandidateMapper.java`
  - 映射 PDF 缓存字段。
- Modify: `admin/src/shared/types/candidate.ts`
  - 扩展 `ReportAsset` 类型。
- Modify: `admin/src/shared/api/adminApi.ts`
  - 增加缓存、审批、拒绝 API。
- Modify: `admin/src/features/candidates/CandidateReviewPage.tsx`
  - PDF 区块显示缓存状态并提供操作按钮。
- Modify: `admin/src/mock/candidates.ts`
  - Mock PDF 资产补齐字段。
- Test: `backend/src/test/java/com/pulsebrief/ingestion/IngestionSchemaTest.java`
- Test: `backend/src/test/java/com/pulsebrief/ingestion/PdfAssetCacheServiceTest.java`
- Test: `backend/src/test/java/com/pulsebrief/admin/AdminCandidateControllerTest.java`
- Test: `admin/src/shared/api/adminApi.test.ts`
- Test: `admin/src/App.test.tsx`

## Task 1: Schema and Domain TDD

- [ ] **Step 1: Write failing schema assertions**

Add assertions in `backend/src/test/java/com/pulsebrief/ingestion/IngestionSchemaTest.java`:

```java
assertThat(tableExists("report_asset_file")).isTrue();
assertThat(columnExists("report_asset", "asset_file_id")).isTrue();
assertThat(columnExists("report_asset", "license_note")).isTrue();
assertThat(columnExists("report_asset", "cache_status")).isTrue();
assertThat(columnExists("report_asset", "cache_error_message")).isTrue();
assertThat(columnExists("report_asset", "cache_completed_at")).isTrue();
assertThat(columnExists("report_asset", "review_note")).isTrue();
assertThat(columnExists("report_asset_file", "storage_path")).isTrue();
assertThat(columnExists("report_asset_file", "mime_type")).isTrue();
```

- [ ] **Step 2: Run RED**

Run:

```powershell
.\mvnw.cmd "-Dtest=IngestionSchemaTest" test
```

Expected: FAIL because `report_asset_file` and new columns do not exist.

- [ ] **Step 3: Add migration and entities**

Create `V11__report_asset_cache.sql`, `ReportAssetFile`, `ReportAssetFileRepository`, and extend `ReportAsset`.

- [ ] **Step 4: Run GREEN**

Run:

```powershell
.\mvnw.cmd "-Dtest=IngestionSchemaTest" test
```

Expected: PASS.

## Task 2: PDF Cache Service TDD

- [ ] **Step 1: Write failing service tests**

Create `backend/src/test/java/com/pulsebrief/ingestion/PdfAssetCacheServiceTest.java` covering:

```java
@Test
void cachesAuthorizedPdfAndStoresPhysicalFileOnce()

@Test
void skipsPdfCacheWhenSourceIsNotAuthorized()

@Test
void reusesCachedFileForSamePdfHashAcrossCandidateAssets()
```

The test configuration provides:

```java
@Bean
@Primary
PdfDownloadClient fixturePdfDownloadClient() {
    return url -> new DownloadedPdf("fixture-report.pdf", "application/pdf", "%PDF-1.4\nfixture\n%%EOF".getBytes(StandardCharsets.UTF_8));
}
```

- [ ] **Step 2: Run RED**

Run:

```powershell
.\mvnw.cmd "-Dtest=PdfAssetCacheServiceTest" test
```

Expected: compilation failure or test failure because cache service does not exist.

- [ ] **Step 3: Implement minimal service**

Implement `PdfAssetCacheService.cacheAsset(candidateId, assetId)` with these observable outcomes:

- Unauthorized source returns `cacheStatus = SKIPPED` and does not create `report_asset_file`.
- Authorized source downloads bytes, hashes them with SHA-256, writes one local PDF file, creates or reuses `report_asset_file`, links `report_asset.asset_file_id`, sets `cacheStatus = SUCCESS`.
- Download/runtime failures set `cacheStatus = FAILED` with a short error message.

- [ ] **Step 4: Run GREEN**

Run:

```powershell
.\mvnw.cmd "-Dtest=PdfAssetCacheServiceTest,ReportAssetRegistrationServiceTest" test
```

Expected: PASS.

## Task 3: Admin API TDD

- [ ] **Step 1: Write failing MockMvc tests**

Extend `backend/src/test/java/com/pulsebrief/admin/AdminCandidateControllerTest.java`:

```java
@Test
void cachesAndApprovesAuthorizedPdfAssetBeforePublish()

@Test
void rejectsPdfAssetAndAllowsArticlePublishWithoutPdfEntry()
```

Expected endpoint shapes:

```http
POST /api/admin/candidates/{candidateId}/report-assets/{assetId}/cache
POST /api/admin/candidates/{candidateId}/report-assets/{assetId}/approve
POST /api/admin/candidates/{candidateId}/report-assets/{assetId}/reject
```

- [ ] **Step 2: Run RED**

Run:

```powershell
.\mvnw.cmd "-Dtest=AdminCandidateControllerTest" test
```

Expected: FAIL with 404 or missing methods.

- [ ] **Step 3: Implement Admin endpoints**

Add controller and application service methods:

```java
public AdminReportAssetResponse cacheReportAsset(Long candidateId, Long assetId)
public AdminReportAssetResponse approveReportAsset(Long candidateId, Long assetId, AdminReportAssetActionRequest request)
public AdminReportAssetResponse rejectReportAsset(Long candidateId, Long assetId, AdminReportAssetActionRequest request)
```

- [ ] **Step 4: Run GREEN**

Run:

```powershell
.\mvnw.cmd "-Dtest=AdminCandidateControllerTest" test
```

Expected: PASS.

## Task 4: Admin Frontend TDD

- [ ] **Step 1: Write failing API tests**

Extend `admin/src/shared/api/adminApi.test.ts` to assert:

- `mapReportAsset` keeps `originalUrl`, `fileSizeBytes`, `fileHash`, `licenseNote`, `cacheStatus`, `cacheErrorMessage`, `mimeType`, `cachedAt`, `reviewNote`.
- HTTP client calls cache/approve/reject endpoint paths.

Run:

```powershell
npm test -- --run adminApi
```

Expected: FAIL because fields and client methods are missing.

- [ ] **Step 2: Implement Admin API client**

Add exported functions:

```ts
export function cacheCandidateReportAsset(candidateId: number, assetId: number): Promise<ReportAsset>
export function approveCandidateReportAsset(candidateId: number, assetId: number, reviewNote?: string): Promise<ReportAsset>
export function rejectCandidateReportAsset(candidateId: number, assetId: number, reviewNote?: string): Promise<ReportAsset>
```

- [ ] **Step 3: Write failing UI test**

Extend `admin/src/App.test.tsx` to assert PDF section shows cache status and exposes buttons:

```ts
expect(screen.getByText('NOT_CACHED')).toBeInTheDocument();
expect(screen.getByRole('button', { name: /缓存 PDF/ })).toBeInTheDocument();
expect(screen.getByRole('button', { name: /审批 PDF/ })).toBeInTheDocument();
expect(screen.getByRole('button', { name: /拒绝 PDF/ })).toBeInTheDocument();
```

- [ ] **Step 4: Implement UI**

Update `CandidateReviewPage.tsx` PDF asset block with status metadata and operation buttons.

- [ ] **Step 5: Run GREEN**

Run:

```powershell
npm test -- --run adminApi App
```

Expected: PASS.

## Task 5: Verification, Docs, Commit, Push

- [ ] **Step 1: Update docs**

Update `docs/下一阶段任务清单.md` and `docs/测试方案.md` with阶段 26 第一批实现记录 and verification commands.

- [ ] **Step 2: Backend verification**

Run:

```powershell
.\mvnw.cmd test
```

Expected: PASS.

- [ ] **Step 3: Admin verification**

Run:

```powershell
npm test -- --run
npm run build
```

Expected: PASS.

- [ ] **Step 4: Deploy config and diff checks**

Run:

```powershell
docker compose -f deploy/docker-compose.yml config
git diff --check
```

Expected: both exit 0.

- [ ] **Step 5: Commit and push**

Run:

```powershell
git add backend admin docs
git commit -m "feat: add authorized pdf caching"
git push origin codex/rss-provider-v1
```

Expected: branch push succeeds; GitHub Actions can run on the pushed commit.

## Self-Review

- 设计覆盖：对应 `docs/授权PDF元数据与文件缓存设计.md` 的授权校验、物理文件拆分、缓存状态、Admin 操作、测试策略。
- 明确非目标：PDF 解析、AI 摘要、复杂分类、Flutter PDF 入口全部留到后续阶段。
- 类型一致：后端响应字段和前端 `ReportAsset` 字段统一使用 `cacheStatus`、`cacheErrorMessage`、`mimeType`、`cachedAt`、`reviewNote`。
- 测试优先：每个实现任务先写 RED，再补最小实现，再跑 GREEN。
