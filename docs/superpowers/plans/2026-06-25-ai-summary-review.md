# AI Summary Review Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the first shippable Stage 27 slice: mock AI summary generation, task persistence, Admin API, and Admin generate/apply controls.

**Architecture:** AI summary generation is isolated behind an `AiSummaryProvider` interface and persisted in `ai_summary_task`. The first batch uses deterministic `MOCK` output only, selects authorized content before RSS metadata, and never calls real AI services or PDF parsing.

**Tech Stack:** Spring Boot 3, Flyway, Spring Data JPA, MySQL 8, JUnit 5, AssertJ, React, TypeScript, Vitest.

---

## File Structure

- Create: `backend/src/main/resources/db/migration/V12__ai_summary_task.sql` for the task table.
- Create: `backend/src/main/java/com/pulsebrief/ingestion/domain/AiSummaryTask.java` for persisted task state.
- Create: `backend/src/main/java/com/pulsebrief/ingestion/repository/AiSummaryTaskRepository.java` for latest-task lookup.
- Create: `backend/src/main/java/com/pulsebrief/ingestion/service/AiSummaryInput.java`, `AiSummaryRequest.java`, `AiSummaryProviderResult.java`, `AiSummaryProvider.java`, `MockAiSummaryProvider.java`, `AiSummaryTaskService.java` for service boundaries.
- Create: `backend/src/main/java/com/pulsebrief/admin/api/AdminAiSummaryGenerateRequest.java` and `AdminAiSummaryTaskResponse.java`.
- Modify: `backend/src/main/java/com/pulsebrief/admin/api/AdminCandidateDetailResponse.java`, `AdminCandidateController.java`, `AdminCandidateApplicationService.java`, `AdminCandidateMapper.java`.
- Modify: `backend/src/test/java/com/pulsebrief/ingestion/IngestionSchemaTest.java`.
- Create: `backend/src/test/java/com/pulsebrief/ingestion/AiSummaryTaskServiceTest.java`.
- Modify: `backend/src/test/java/com/pulsebrief/admin/AdminCandidateControllerTest.java`.
- Modify: `admin/src/shared/types/candidate.ts`, `admin/src/shared/api/adminApi.ts`, `admin/src/shared/api/adminApi.test.ts`, `admin/src/features/candidates/CandidateReviewPage.tsx`, `admin/src/mock/candidates.ts`, `admin/src/App.test.tsx`.
- Modify: `docs/测试方案.md` to mark Stage 27 first-batch commands and CI boundary.

### Task 1: Schema And Domain

**Files:**
- Create: `backend/src/main/resources/db/migration/V12__ai_summary_task.sql`
- Create: `backend/src/main/java/com/pulsebrief/ingestion/domain/AiSummaryTask.java`
- Create: `backend/src/main/java/com/pulsebrief/ingestion/repository/AiSummaryTaskRepository.java`
- Test: `backend/src/test/java/com/pulsebrief/ingestion/IngestionSchemaTest.java`

- [x] **Step 1: Write the failing schema assertion**

```java
assertThat(tableExists("ai_summary_task")).isTrue();
assertThat(columnExists("ai_summary_task", "candidate_article_id")).isTrue();
assertThat(columnExists("ai_summary_task", "input_source_type")).isTrue();
assertThat(columnExists("ai_summary_task", "provider_type")).isTrue();
assertThat(columnExists("ai_summary_task", "task_status")).isTrue();
assertThat(columnExists("ai_summary_task", "generated_summary")).isTrue();
```

- [x] **Step 2: Run schema test and verify RED**

Run: `cd backend && .\mvnw.cmd "-Dtest=IngestionSchemaTest" test`
Expected: FAIL because `ai_summary_task` does not exist.

- [x] **Step 3: Add migration and JPA model**

```sql
create table ai_summary_task (
    id bigint primary key auto_increment,
    candidate_article_id bigint not null,
    raw_news_item_id bigint not null,
    input_source_type varchar(32) not null,
    input_ref_id bigint null,
    input_hash varchar(64) not null,
    input_preview varchar(1000) null,
    provider_type varchar(32) not null,
    model_name varchar(128) not null,
    prompt_version varchar(64) not null,
    task_status varchar(32) not null,
    generated_summary text null,
    generated_key_points text null,
    generated_impact_analysis text null,
    token_prompt_count int null,
    token_completion_count int null,
    error_message varchar(1000) null,
    requested_by varchar(64) null,
    started_at datetime null,
    finished_at datetime null,
    created_at datetime not null,
    updated_at datetime not null
);
```

- [x] **Step 4: Run schema test and verify GREEN**

Run: `cd backend && .\mvnw.cmd "-Dtest=IngestionSchemaTest" test`
Expected: PASS.

### Task 2: Mock AI Summary Service

**Files:**
- Create: service records/interfaces under `backend/src/main/java/com/pulsebrief/ingestion/service/`
- Create: `backend/src/test/java/com/pulsebrief/ingestion/AiSummaryTaskServiceTest.java`

- [x] **Step 1: Write failing service tests**

```java
@Test
void generatesMockSummaryFromRssSummary() {
    CandidateArticle candidate = createCandidate("rss-summary");
    AiSummaryTask task = aiSummaryTaskService.generate(candidate.getId(), "AUTO", "MOCK", "candidate-summary-v1");
    assertThat(task.getTaskStatus()).isEqualTo("SUCCESS");
    assertThat(task.getInputSourceType()).isEqualTo("RSS_SUMMARY");
    assertThat(task.getGeneratedSummary()).contains(candidate.getTitle());
}

@Test
void prefersAuthorizedContentSnippetOverRssSummary() {
    CandidateArticle candidate = createCandidate("content-snippet");
    saveSuccessfulRawContent(candidate, "SNIPPET", "FULLTEXT_ALLOWED", "Authorized snippet body for AI summary.");
    AiSummaryTask task = aiSummaryTaskService.generate(candidate.getId(), "AUTO", "MOCK", "candidate-summary-v1");
    assertThat(task.getTaskStatus()).isEqualTo("SUCCESS");
    assertThat(task.getInputSourceType()).isEqualTo("CONTENT_SNIPPET");
}
```

- [x] **Step 2: Run service tests and verify RED**

Run: `cd backend && .\mvnw.cmd "-Dtest=AiSummaryTaskServiceTest" test`
Expected: compilation FAIL because the service classes do not exist.

- [x] **Step 3: Implement minimal service**

```java
AiSummaryTask generate(Long candidateId, String inputSourceType, String providerType, String promptVersion)
AiSummaryTask apply(Long candidateId, Long taskId)
```

Use latest successful `raw_news_content` with `FULLTEXT_ALLOWED` or `SNIPPET_ALLOWED`, otherwise fall back to RSS summary. Store deterministic mock summary, key points, impact analysis, model `mock-v1`, token counts `0`, and status `SUCCESS`; store `SKIPPED` if input text is blank.

- [x] **Step 4: Run service tests and verify GREEN**

Run: `cd backend && .\mvnw.cmd "-Dtest=AiSummaryTaskServiceTest" test`
Expected: PASS.

### Task 3: Admin Backend API

**Files:**
- Create: `backend/src/main/java/com/pulsebrief/admin/api/AdminAiSummaryGenerateRequest.java`
- Create: `backend/src/main/java/com/pulsebrief/admin/api/AdminAiSummaryTaskResponse.java`
- Modify: `AdminCandidateDetailResponse.java`, `AdminCandidateController.java`, `AdminCandidateApplicationService.java`, `AdminCandidateMapper.java`
- Test: `backend/src/test/java/com/pulsebrief/admin/AdminCandidateControllerTest.java`

- [x] **Step 1: Write failing controller tests**

```java
mockMvc.perform(post("/api/admin/candidates/{id}/ai-summary/generate", candidate.getId())
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"inputSourceType\":\"AUTO\",\"providerType\":\"MOCK\",\"promptVersion\":\"candidate-summary-v1\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("SUCCESS"))
        .andExpect(jsonPath("$.data.providerType").value("MOCK"));
```

- [x] **Step 2: Run controller tests and verify RED**

Run: `cd backend && .\mvnw.cmd "-Dtest=AdminCandidateControllerTest" test`
Expected: FAIL with 404 or missing response fields.

- [x] **Step 3: Add endpoints and response mapping**

```http
POST /api/admin/candidates/{id}/ai-summary/generate
POST /api/admin/candidates/{id}/ai-summary/{taskId}/apply
GET  /api/admin/candidates/{id} -> aiSummaryTask
```

`apply` validates the task belongs to the candidate and returns the generated draft to the Admin UI; persistence remains in `ai_summary_task`, not candidate draft columns.

- [x] **Step 4: Run controller tests and verify GREEN**

Run: `cd backend && .\mvnw.cmd "-Dtest=AdminCandidateControllerTest,AiSummaryTaskServiceTest,IngestionSchemaTest" test`
Expected: PASS.

### Task 4: Admin Frontend Generate And Apply

**Files:**
- Modify: `admin/src/shared/types/candidate.ts`
- Modify: `admin/src/shared/api/adminApi.ts`
- Modify: `admin/src/shared/api/adminApi.test.ts`
- Modify: `admin/src/features/candidates/CandidateReviewPage.tsx`
- Modify: `admin/src/mock/candidates.ts`
- Modify: `admin/src/App.test.tsx`

- [x] **Step 1: Write failing API/UI tests**

```ts
expect(candidate.aiSummaryTask?.status).toBe('SUCCESS')
await user.click(screen.getByRole('button', { name: /生成 AI 摘要/ }))
await user.click(screen.getByRole('button', { name: /采用草稿/ }))
expect(screen.getByText(/Mock AI 摘要/)).toBeInTheDocument()
```

- [x] **Step 2: Run Admin tests and verify RED**

Run: `cd admin && npm test -- --run adminApi.test.ts App.test.tsx`
Expected: FAIL because AI summary task mapping and buttons do not exist.

- [x] **Step 3: Implement API client and UI controls**

Add `generateCandidateAiSummary(id)` and `applyCandidateAiSummary(id, taskId)`. The mock client creates deterministic task output; the real client calls the new endpoints. The review page shows task status and lets Admin adopt generated summary/key points/impact into the publish payload.

- [x] **Step 4: Run Admin tests and verify GREEN**

Run: `cd admin && npm test -- --run adminApi.test.ts App.test.tsx`
Expected: PASS.

### Task 5: Regression And Delivery

**Files:**
- Modify: `docs/测试方案.md`

- [x] **Step 1: Update Stage 27 test record**

Document that first batch uses mock Provider only and CI does not call real AI APIs.

- [x] **Step 2: Run targeted backend regression**

Run: `cd backend && .\mvnw.cmd "-Dtest=AiSummaryTaskServiceTest,AdminCandidateControllerTest,IngestionSchemaTest,PdfAssetCacheLiveSmokeTest" test`
Expected: PASS, with `PdfAssetCacheLiveSmokeTest` skipped unless explicitly enabled.

- [x] **Step 3: Run full backend regression**

Run: `cd backend && .\mvnw.cmd test`
Expected: PASS.

- [x] **Step 4: Run Admin regression**

Run:

```powershell
cd admin
npm test -- --run
npm run lint
npm run build
```

Expected: PASS.

- [x] **Step 5: Commit and push**

```powershell
git status --short
git add backend admin docs
git commit -m "feat: add mock ai summary review"
git push origin codex/rss-provider-v1
```

Expected: push succeeds over SSH and GitHub Actions starts for the branch.
