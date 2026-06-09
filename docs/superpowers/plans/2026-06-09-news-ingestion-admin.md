# News Ingestion Admin Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a compliant V1 real-news ingestion, candidate review, and Admin publishing loop for PulseBrief.

**Architecture:** Spring Boot owns ingestion providers, raw news storage, duplicate detection, candidate generation, and Admin APIs. React Admin later consumes Admin APIs for review and publishing, while Flutter continues to read only published mobile-facing APIs.

**Tech Stack:** Spring Boot 3.5, Java 17, Spring Data JPA, Flyway, MySQL 8, springdoc-openapi, React, Vite, TypeScript, Flutter.

---

## File Structure

- Create: `docs/真实资讯采集与Admin审核发布整体任务清单.md`
- Create: `docs/真实资讯数据采集V1设计.md`
- Modify: `docs/下一阶段任务清单.md`
- Create: `backend/src/main/java/com/pulsebrief/ingestion/provider/NewsIngestionProvider.java`
- Create: `backend/src/main/java/com/pulsebrief/ingestion/provider/RawNewsPayload.java`
- Create: `backend/src/main/java/com/pulsebrief/ingestion/service/IngestionDeduplicationService.java`
- Create: `backend/src/main/java/com/pulsebrief/ingestion/domain/NewsIngestionSource.java`
- Create: `backend/src/main/java/com/pulsebrief/ingestion/domain/NewsIngestionJob.java`
- Create: `backend/src/main/java/com/pulsebrief/ingestion/domain/RawNewsItem.java`
- Create: `backend/src/main/java/com/pulsebrief/ingestion/domain/CandidateArticle.java`
- Create: `backend/src/main/resources/db/migration/V3__news_ingestion.sql`
- Create: `backend/src/test/java/com/pulsebrief/ingestion/IngestionDeduplicationServiceTest.java`
- Create later: `admin/`

## Task 1: Planning Documents

- [x] **Step 1: Create the overall task list**

Create `docs/真实资讯采集与Admin审核发布整体任务清单.md` with phases 11-20:

```text
Stage 11: Real news ingestion V1 design
Stage 12: Provider adapter layer
Stage 13: Raw ingestion persistence and deduplication
Stage 14: Candidate article generation
Stage 15: Admin API review and publish design
Stage 16: React Admin foundation
Stage 17: Candidate review UI
Stage 18: Daily digest review
Stage 19: Operation quality and monitoring
Stage 20: Final verification
```

- [x] **Step 2: Create the V1 ingestion design**

Create `docs/真实资讯数据采集V1设计.md` covering:

```text
background
goals and non-goals
scope
data model
permission model
backend plan
frontend impact
tests
risks and phased rollout
```

- [x] **Step 3: Update the project task list**

Update `docs/下一阶段任务清单.md` with stages 11-20 and mark stage 11 planning as complete.

## Task 2: Backend Ingestion Domain Design

- [x] **Step 1: Write a failing Flyway schema expectation test**

Create `backend/src/test/java/com/pulsebrief/ingestion/IngestionSchemaTest.java`:

```java
package com.pulsebrief.ingestion;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class IngestionSchemaTest {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void createsIngestionTables() {
        assertThat(tableExists("news_ingestion_source")).isTrue();
        assertThat(tableExists("news_ingestion_job")).isTrue();
        assertThat(tableExists("raw_news_item")).isTrue();
        assertThat(tableExists("candidate_article")).isTrue();
    }

    private boolean tableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.tables where table_schema = database() and table_name = ?",
                Integer.class,
                tableName
        );
        return count != null && count > 0;
    }
}
```

- [x] **Step 2: Run the targeted test and verify failure**

Run:

```powershell
$env:JAVA_HOME='D:\Dev\jdk\jdk-17'
$env:Path='D:\Dev\jdk\jdk-17\bin;' + $env:Path
.\mvnw.cmd -Dtest=IngestionSchemaTest test
```

Expected: test fails because the four ingestion tables do not exist.

- [x] **Step 3: Add Flyway migration**

Create `backend/src/main/resources/db/migration/V3__news_ingestion.sql` with tables:

```sql
create table news_ingestion_source (
    id bigint primary key auto_increment,
    code varchar(64) not null unique,
    name varchar(128) not null,
    provider_type varchar(32) not null,
    base_url varchar(512) not null,
    default_category_code varchar(64) null,
    enabled boolean not null default true,
    rate_limit_per_hour int not null default 60,
    created_at datetime not null,
    updated_at datetime not null
);

create table news_ingestion_job (
    id bigint primary key auto_increment,
    source_code varchar(64) not null,
    trigger_type varchar(32) not null,
    status varchar(32) not null,
    started_at datetime not null,
    finished_at datetime null,
    fetched_count int not null default 0,
    new_count int not null default 0,
    duplicate_count int not null default 0,
    candidate_count int not null default 0,
    error_message varchar(1024) null,
    created_at datetime not null,
    updated_at datetime not null
);

create table raw_news_item (
    id bigint primary key auto_increment,
    source_code varchar(64) not null,
    provider_item_id varchar(128) null,
    title varchar(512) not null,
    summary varchar(2000) null,
    source_name varchar(128) not null,
    original_url varchar(1024) not null,
    image_url varchar(1024) null,
    published_at datetime null,
    fetched_at datetime not null,
    language varchar(16) null,
    country varchar(32) null,
    raw_payload json null,
    content_hash varchar(128) not null,
    status varchar(32) not null,
    duplicate_of_id bigint null,
    created_at datetime not null,
    updated_at datetime not null,
    unique key uk_raw_news_item_original_url (original_url),
    key idx_raw_news_item_status (status),
    key idx_raw_news_item_content_hash (content_hash)
);

create table candidate_article (
    id bigint primary key auto_increment,
    raw_news_item_id bigint not null,
    title varchar(512) not null,
    summary varchar(2000) null,
    category_code varchar(64) null,
    source_name varchar(128) not null,
    original_url varchar(1024) not null,
    published_at datetime null,
    status varchar(32) not null,
    review_note varchar(1000) null,
    published_article_id bigint null,
    created_at datetime not null,
    updated_at datetime not null,
    unique key uk_candidate_article_raw_news_item_id (raw_news_item_id),
    key idx_candidate_article_status (status)
);
```

- [x] **Step 4: Run the targeted test and verify pass**

Run:

```powershell
.\mvnw.cmd -Dtest=IngestionSchemaTest test
```

Expected: test passes.

## Task 3: Provider Abstraction And Fixture Mapping

- [ ] **Step 1: Add failing provider mapping test**

Create `backend/src/test/java/com/pulsebrief/ingestion/provider/FixtureNewsIngestionProviderTest.java`:

```java
package com.pulsebrief.ingestion.provider;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FixtureNewsIngestionProviderTest {
    @Test
    void mapsFixtureItemsIntoRawPayloads() {
        FixtureNewsIngestionProvider provider = new FixtureNewsIngestionProvider();

        List<RawNewsPayload> payloads = provider.fetchSample();

        assertThat(payloads).hasSize(2);
        assertThat(payloads.get(0).title()).isEqualTo("AI infrastructure investment remains resilient");
        assertThat(payloads.get(0).originalUrl()).isEqualTo("https://example.com/ai-infra");
        assertThat(payloads.get(0).sourceName()).isEqualTo("Example Markets");
    }
}
```

- [ ] **Step 2: Implement provider interface and fixture provider**

Create `backend/src/main/java/com/pulsebrief/ingestion/provider/RawNewsPayload.java`:

```java
package com.pulsebrief.ingestion.provider;

import java.time.OffsetDateTime;

public record RawNewsPayload(
        String providerItemId,
        String title,
        String summary,
        String sourceName,
        String originalUrl,
        String imageUrl,
        OffsetDateTime publishedAt,
        String language,
        String country,
        String rawPayload
) {
}
```

Create `backend/src/main/java/com/pulsebrief/ingestion/provider/NewsIngestionProvider.java`:

```java
package com.pulsebrief.ingestion.provider;

import java.util.List;

public interface NewsIngestionProvider {
    String providerType();

    List<RawNewsPayload> fetchSample();
}
```

Create `backend/src/main/java/com/pulsebrief/ingestion/provider/FixtureNewsIngestionProvider.java`:

```java
package com.pulsebrief.ingestion.provider;

import java.time.OffsetDateTime;
import java.util.List;

public class FixtureNewsIngestionProvider implements NewsIngestionProvider {
    @Override
    public String providerType() {
        return "FIXTURE";
    }

    @Override
    public List<RawNewsPayload> fetchSample() {
        return List.of(
                new RawNewsPayload(
                        "fixture-1",
                        "AI infrastructure investment remains resilient",
                        "Public market commentary highlights continued AI infrastructure demand.",
                        "Example Markets",
                        "https://example.com/ai-infra",
                        null,
                        OffsetDateTime.parse("2026-06-09T09:00:00+08:00"),
                        "en",
                        "US",
                        "{\"id\":\"fixture-1\"}"
                ),
                new RawNewsPayload(
                        "fixture-2",
                        "Central bank officials keep cautious tone",
                        "Investors reassess rate expectations after public remarks.",
                        "Example Policy",
                        "https://example.com/rates",
                        null,
                        OffsetDateTime.parse("2026-06-09T10:00:00+08:00"),
                        "en",
                        "US",
                        "{\"id\":\"fixture-2\"}"
                )
        );
    }
}
```

- [ ] **Step 3: Run provider test**

Run:

```powershell
.\mvnw.cmd -Dtest=FixtureNewsIngestionProviderTest test
```

Expected: test passes.

## Task 4: Deduplication Service

- [ ] **Step 1: Add failing deduplication test**

Create `backend/src/test/java/com/pulsebrief/ingestion/IngestionDeduplicationServiceTest.java`:

```java
package com.pulsebrief.ingestion;

import com.pulsebrief.ingestion.service.IngestionDeduplicationService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IngestionDeduplicationServiceTest {
    private final IngestionDeduplicationService service = new IngestionDeduplicationService();

    @Test
    void normalizesTitleForStableHashing() {
        String first = service.contentHash("Example Source", " AI: Market   Rally! ", "2026-06-09");
        String second = service.contentHash("Example Source", "ai market rally", "2026-06-09");

        assertThat(first).isEqualTo(second);
    }
}
```

- [ ] **Step 2: Implement deduplication service**

Create `backend/src/main/java/com/pulsebrief/ingestion/service/IngestionDeduplicationService.java`:

```java
package com.pulsebrief.ingestion.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public class IngestionDeduplicationService {
    public String contentHash(String sourceName, String title, String publishDate) {
        String normalized = normalize(sourceName) + "|" + normalize(title) + "|" + normalize(publishDate);
        return sha256(normalized);
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase()
                .replaceAll("[\\p{Punct}]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
```

- [ ] **Step 3: Run deduplication test**

Run:

```powershell
.\mvnw.cmd -Dtest=IngestionDeduplicationServiceTest test
```

Expected: test passes.

## Task 5: Documentation And Verification

- [ ] **Step 1: Update `docs/下一阶段任务清单.md`**

Mark stage 12 or 13 progress according to the completed implementation.

- [ ] **Step 2: Run backend verification**

Run:

```powershell
$env:JAVA_HOME='D:\Dev\jdk\jdk-17'
$env:Path='D:\Dev\jdk\jdk-17\bin;' + $env:Path
.\mvnw.cmd test
```

Expected: all backend tests pass.

- [ ] **Step 3: Run mobile verification**

Run in `mobile/`:

```powershell
$env:JAVA_HOME='D:\Dev\jdk\jdk-17'
$env:ANDROID_HOME='D:\Dev\Android\Sdk'
$env:ANDROID_SDK_ROOT='D:\Dev\Android\Sdk'
$env:ANDROID_USER_HOME='D:\Dev\Android\.android'
$env:ANDROID_AVD_HOME='D:\Dev\Android\Avd'
$env:GRADLE_USER_HOME='D:\Dev\Gradle'
$env:Path='D:\Dev\flutter\bin;D:\Dev\jdk\jdk-17\bin;D:\Dev\Android\Sdk\platform-tools;D:\Dev\Android\Sdk\emulator;D:\Dev\Android\Sdk\cmdline-tools\latest\bin;' + $env:Path
flutter analyze
flutter test
```

Expected: analyze has no issues and tests pass.

- [ ] **Step 4: Commit and push**

Run:

```powershell
git add backend docs
git commit -m "feat: design news ingestion workflow"
git push origin main
```
