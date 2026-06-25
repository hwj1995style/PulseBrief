# Authorized Content Fetching Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add the first implementation slice for authorized snippet/fulltext fetching without changing RSS Provider responsibilities.

**Architecture:** Add `raw_news_content` as a separate content-enhancement table linked to `raw_news_item`. Implement a `ContentFetchService` that checks source license policy, fetches HTML through an injectable client, extracts clean text, stores snippet/fulltext content, and exposes content state through Admin candidate detail and a manual Admin fetch endpoint.

**Tech Stack:** Spring Boot 3, Spring Data JPA, Flyway, MockMvc, React Admin TypeScript API client and Vitest.

---

### Task 1: Backend Schema And Domain

**Files:**
- Create: `backend/src/main/resources/db/migration/V10__raw_news_content.sql`
- Create: `backend/src/main/java/com/pulsebrief/ingestion/domain/RawNewsContent.java`
- Create: `backend/src/main/java/com/pulsebrief/ingestion/repository/RawNewsContentRepository.java`
- Modify: `backend/src/test/java/com/pulsebrief/ingestion/IngestionSchemaTest.java`

- [ ] **Step 1: Write failing schema/entity test**

Add assertions that `raw_news_content` exists and includes `content_text_hash`, `fetch_status`, and `raw_news_item_id`.

- [ ] **Step 2: Run the schema test to verify RED**

Run:

```powershell
cd backend
.\mvnw.cmd "-Dtest=IngestionSchemaTest" test
```

Expected: fail because `raw_news_content` does not exist.

- [ ] **Step 3: Add migration, entity, and repository**

Create `raw_news_content` with `raw_news_item_id`, `source_code`, `capture_mode`, `content_text`, `content_text_hash`, `license_policy`, `license_note`, `fetch_status`, `error_message`, `fetched_at`, `created_at`, and `updated_at`.

- [ ] **Step 4: Run schema test to verify GREEN**

Run:

```powershell
cd backend
.\mvnw.cmd "-Dtest=IngestionSchemaTest" test
```

Expected: pass.

### Task 2: Content Fetch Service

**Files:**
- Create: `backend/src/main/java/com/pulsebrief/ingestion/service/ContentFetchMode.java`
- Create: `backend/src/main/java/com/pulsebrief/ingestion/service/ContentFetchResult.java`
- Create: `backend/src/main/java/com/pulsebrief/ingestion/service/HtmlContentClient.java`
- Create: `backend/src/main/java/com/pulsebrief/ingestion/service/HttpHtmlContentClient.java`
- Create: `backend/src/main/java/com/pulsebrief/ingestion/service/HtmlContentExtractor.java`
- Create: `backend/src/main/java/com/pulsebrief/ingestion/service/ContentFetchService.java`
- Create: `backend/src/test/java/com/pulsebrief/ingestion/ContentFetchServiceTest.java`

- [ ] **Step 1: Write failing service tests**

Cover these behaviors:

1. `SUMMARY_ONLY` source is skipped and does not save content.
2. `SNIPPET_ALLOWED` source saves a snippet from fixture HTML.
3. `FULLTEXT_ALLOWED` source saves longer fulltext from fixture HTML.
4. Missing `license_note` skips content fetch.
5. Expired raw item skips content fetch.

- [ ] **Step 2: Run service test to verify RED**

Run:

```powershell
cd backend
.\mvnw.cmd "-Dtest=ContentFetchServiceTest" test
```

Expected: fail because service classes do not exist.

- [ ] **Step 3: Implement minimal service**

Use an injectable `HtmlContentClient` so tests can provide fixture HTML without real network. Use `HtmlContentExtractor` with conservative HTML tag cleanup. Keep length limits small and explicit: snippet max 1000 chars, fulltext max 20000 chars.

- [ ] **Step 4: Run service test to verify GREEN**

Run:

```powershell
cd backend
.\mvnw.cmd "-Dtest=ContentFetchServiceTest" test
```

Expected: pass.

### Task 3: Admin Candidate Content API

**Files:**
- Create: `backend/src/main/java/com/pulsebrief/admin/api/AdminCandidateContentFetchRequest.java`
- Create: `backend/src/main/java/com/pulsebrief/admin/api/AdminCandidateContentResponse.java`
- Modify: `backend/src/main/java/com/pulsebrief/admin/api/AdminCandidateDetailResponse.java`
- Modify: `backend/src/main/java/com/pulsebrief/admin/api/AdminCandidateController.java`
- Modify: `backend/src/main/java/com/pulsebrief/admin/service/AdminCandidateApplicationService.java`
- Modify: `backend/src/test/java/com/pulsebrief/admin/AdminCandidateControllerTest.java`

- [ ] **Step 1: Write failing Admin API tests**

Add tests for:

1. candidate detail returns `content.fetchStatus` after content exists.
2. `POST /api/admin/candidates/{id}/content/fetch` triggers authorized snippet fetch.
3. unauthorized source returns/skips with clear `SKIPPED` result and no saved content.

- [ ] **Step 2: Run Admin candidate test to verify RED**

Run:

```powershell
cd backend
.\mvnw.cmd "-Dtest=AdminCandidateControllerTest" test
```

Expected: fail because endpoint/response fields do not exist.

- [ ] **Step 3: Implement Admin API**

Add content response to candidate detail. Add manual fetch endpoint that calls `ContentFetchService.fetchForCandidate(candidateId, mode)`.

- [ ] **Step 4: Run Admin candidate test to verify GREEN**

Run:

```powershell
cd backend
.\mvnw.cmd "-Dtest=AdminCandidateControllerTest" test
```

Expected: pass.

### Task 4: React Admin API And Candidate Detail UI

**Files:**
- Modify: `admin/src/shared/types/candidate.ts`
- Modify: `admin/src/shared/api/adminApi.ts`
- Modify: `admin/src/shared/api/adminApi.test.ts`
- Modify: `admin/src/features/candidates/CandidateReviewPage.tsx`
- Modify: `admin/src/App.test.tsx`

- [ ] **Step 1: Write failing Admin API/UI tests**

Cover content response mapping and candidate detail rendering of content fetch status.

- [ ] **Step 2: Run Admin tests to verify RED**

Run:

```powershell
cd admin
npm test -- --run adminApi.test.ts App.test.tsx
```

Expected: fail because content types and UI do not exist.

- [ ] **Step 3: Implement TypeScript mapping and UI**

Add `content` to `AdminCandidate`, map backend detail content, add `fetchCandidateContent(id, mode)` API client method, and show content status/preview in candidate detail. Keep UI compact and consistent with existing candidate detail panels.

- [ ] **Step 4: Run Admin tests to verify GREEN**

Run:

```powershell
cd admin
npm test -- --run adminApi.test.ts App.test.tsx
```

Expected: pass.

### Task 5: Documentation And Verification

**Files:**
- Modify: `docs/下一阶段任务清单.md`
- Modify: `docs/测试方案.md`
- Modify: `docs/授权正文片段与全文抓取服务设计.md`

- [ ] **Step 1: Update docs**

Mark the implementation slice as complete only after tests pass. Record that real webpage fetch remains local explicit smoke only.

- [ ] **Step 2: Run final verification**

Run:

```powershell
cd backend
.\mvnw.cmd test

cd ..\admin
npm test -- --run
npm run lint
npm run build

cd ..
docker compose -f deploy/docker-compose.yml config
git diff --check
```

Expected: all commands pass.
