# RSS Provider V1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the Stage 23 real RSS metadata provider, keep CI fixture-only, and provide a guarded local live-smoke path for one real RSS feed.

**Architecture:** Add a Spring `RssNewsIngestionProvider` that implements the existing `NewsIngestionProvider` interface and maps RSS/Atom entries into `RawNewsPayload`. Keep full text, PDF, AI summarization, and complex classification out of the provider. Tests use local fixture XML; real RSS access is only through an explicit local smoke test gate.

**Tech Stack:** Spring Boot 3.5.7, Java 17, ROME `com.rometools:rome:2.1.0`, JUnit 5, AssertJ, MySQL-backed existing ingestion tests.

---

## File Map

- Modify: `backend/pom.xml`
  - Add `rome.version` property and the `com.rometools:rome` dependency.
- Create: `backend/src/main/java/com/pulsebrief/ingestion/provider/RssNewsIngestionProvider.java`
  - Parse RSS/Atom from configured feed URLs or test-supplied feed content and return `RawNewsPayload`.
- Create: `backend/src/main/java/com/pulsebrief/ingestion/provider/RssFeedClient.java`
  - Fetch feed XML over HTTP for real local runs. Keep network behavior isolated for testability.
- Create: `backend/src/main/java/com/pulsebrief/ingestion/provider/RssFeedParser.java`
  - Convert RSS/Atom XML into `RawNewsPayload` using ROME.
- Create: `backend/src/test/java/com/pulsebrief/ingestion/provider/RssNewsIngestionProviderTest.java`
  - Fixture-only unit tests for RSS, Atom, invalid entries, keyword filtering, page size, and date parse behavior.
- Create: `backend/src/test/resources/ingestion/rss/sample-feed.xml`
  - RSS 2.0 fixture with valid entries, missing guid, missing title, and invalid date.
- Create: `backend/src/test/resources/ingestion/rss/sample-atom-feed.xml`
  - Atom fixture for compatibility.
- Modify: `backend/src/test/java/com/pulsebrief/ingestion/provider/NewsIngestionProviderContextTest.java`
  - Assert both `FIXTURE` and `RSS` providers are registered.
- Create: `backend/src/test/java/com/pulsebrief/ingestion/provider/RssLiveSmokeTest.java`
  - Disabled-by-default live smoke test guarded by `PULSEBRIEF_RSS_LIVE_TEST_ENABLED=true`.
- Modify: `docs/下一阶段任务清单.md`
  - Mark Stage 23 implementation status after code and live smoke verification.
- Modify: `docs/测试方案.md`
  - Add fixture-only automated test command and manual live smoke command.

## Task 1: Add RSS Fixture Tests First

**Files:**
- Create: `backend/src/test/resources/ingestion/rss/sample-feed.xml`
- Create: `backend/src/test/resources/ingestion/rss/sample-atom-feed.xml`
- Create: `backend/src/test/java/com/pulsebrief/ingestion/provider/RssNewsIngestionProviderTest.java`

- [ ] **Step 1: Add RSS fixture**

Create `backend/src/test/resources/ingestion/rss/sample-feed.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<rss version="2.0">
  <channel>
    <title>PulseBrief Test RSS</title>
    <link>https://example.org/rss</link>
    <description>Fixture feed for RSS provider tests.</description>
    <language>en-us</language>
    <item>
      <guid>rss-ai-chip-001</guid>
      <title>AI chip supply expands</title>
      <link>https://example.org/news/ai-chip-supply</link>
      <description><![CDATA[Chip supply news <b>summary</b>.]]></description>
      <pubDate>Mon, 15 Jun 2026 08:30:00 GMT</pubDate>
    </item>
    <item>
      <title>Central bank comments steady markets</title>
      <link>https://example.org/news/central-bank-comments</link>
      <description>Policy summary.</description>
      <pubDate>Mon, 15 Jun 2026 09:00:00 GMT</pubDate>
    </item>
    <item>
      <guid>rss-invalid-date</guid>
      <title>Invalid date still maps</title>
      <link>https://example.org/news/invalid-date</link>
      <description>Invalid date summary.</description>
      <pubDate>not-a-date</pubDate>
    </item>
    <item>
      <guid>rss-missing-title</guid>
      <link>https://example.org/news/missing-title</link>
      <description>This entry must be skipped.</description>
      <pubDate>Mon, 15 Jun 2026 10:00:00 GMT</pubDate>
    </item>
  </channel>
</rss>
```

- [ ] **Step 2: Add Atom fixture**

Create `backend/src/test/resources/ingestion/rss/sample-atom-feed.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<feed xmlns="http://www.w3.org/2005/Atom">
  <title>PulseBrief Test Atom</title>
  <id>https://example.org/atom</id>
  <updated>2026-06-15T09:30:00Z</updated>
  <entry>
    <id>atom-market-001</id>
    <title>Markets digest higher infrastructure spending</title>
    <link href="https://example.org/atom/markets-infra"/>
    <summary>Atom summary text.</summary>
    <updated>2026-06-15T09:30:00Z</updated>
  </entry>
</feed>
```

- [ ] **Step 3: Write the failing provider tests**

Create `backend/src/test/java/com/pulsebrief/ingestion/provider/RssNewsIngestionProviderTest.java`:

```java
package com.pulsebrief.ingestion.provider;

import com.pulsebrief.ingestion.service.IngestionRequest;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RssNewsIngestionProviderTest {
    private final RssFeedParser parser = new RssFeedParser();

    @Test
    void mapsRssFeedItemsIntoRawPayloadsWithoutNetwork() throws Exception {
        List<RawNewsPayload> payloads = parser.parse(
                fixture("sample-feed.xml"),
                new IngestionRequest("rss-test", List.of(), "en", "US", "global", 10)
        );

        assertThat(payloads).hasSize(3);
        RawNewsPayload first = payloads.get(0);
        assertThat(first.providerItemId()).isEqualTo("rss-ai-chip-001");
        assertThat(first.title()).isEqualTo("AI chip supply expands");
        assertThat(first.summary()).isEqualTo("Chip supply news summary.");
        assertThat(first.sourceName()).isEqualTo("PulseBrief Test RSS");
        assertThat(first.originalUrl()).isEqualTo("https://example.org/news/ai-chip-supply");
        assertThat(first.publishedAt()).isEqualTo("2026-06-15T08:30Z");
        assertThat(first.language()).isEqualTo("en");
        assertThat(first.country()).isEqualTo("US");
        assertThat(first.rawPayload()).contains("rss-ai-chip-001");
    }

    @Test
    void usesLinkAsProviderItemIdWhenGuidIsMissing() throws Exception {
        List<RawNewsPayload> payloads = parser.parse(
                fixture("sample-feed.xml"),
                new IngestionRequest("rss-test", List.of("central bank"), "en", "US", "global", 10)
        );

        assertThat(payloads).hasSize(1);
        assertThat(payloads.get(0).providerItemId())
                .isEqualTo("https://example.org/news/central-bank-comments");
    }

    @Test
    void keepsPayloadWhenPublishedDateCannotBeParsed() throws Exception {
        List<RawNewsPayload> payloads = parser.parse(
                fixture("sample-feed.xml"),
                new IngestionRequest("rss-test", List.of("invalid date"), "en", "US", "global", 10)
        );

        assertThat(payloads).hasSize(1);
        assertThat(payloads.get(0).title()).isEqualTo("Invalid date still maps");
        assertThat(payloads.get(0).publishedAt()).isNull();
    }

    @Test
    void appliesPageSizeAfterSkippingInvalidEntries() throws Exception {
        List<RawNewsPayload> payloads = parser.parse(
                fixture("sample-feed.xml"),
                new IngestionRequest("rss-test", List.of(), "en", "US", "global", 2)
        );

        assertThat(payloads).extracting(RawNewsPayload::title)
                .containsExactly("AI chip supply expands", "Central bank comments steady markets");
    }

    @Test
    void mapsAtomFeedItemsIntoRawPayloadsWithoutNetwork() throws Exception {
        List<RawNewsPayload> payloads = parser.parse(
                fixture("sample-atom-feed.xml"),
                new IngestionRequest("atom-test", List.of(), "en", "US", "global", 10)
        );

        assertThat(payloads).hasSize(1);
        assertThat(payloads.get(0).providerItemId()).isEqualTo("atom-market-001");
        assertThat(payloads.get(0).title()).isEqualTo("Markets digest higher infrastructure spending");
        assertThat(payloads.get(0).originalUrl()).isEqualTo("https://example.org/atom/markets-infra");
        assertThat(payloads.get(0).publishedAt()).isEqualTo("2026-06-15T09:30Z");
    }

    private String fixture(String name) throws Exception {
        try (InputStream inputStream = getClass().getResourceAsStream("/ingestion/rss/" + name)) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
```

- [ ] **Step 4: Run test to verify RED**

Run:

```powershell
cd backend
.\mvnw.cmd -Dtest=RssNewsIngestionProviderTest test
```

Expected: compilation failure because `RssFeedParser` does not exist.

## Task 2: Add ROME Dependency And RSS Parser

**Files:**
- Modify: `backend/pom.xml`
- Create: `backend/src/main/java/com/pulsebrief/ingestion/provider/RssFeedParser.java`

- [ ] **Step 1: Add ROME dependency**

Modify `backend/pom.xml`:

```xml
<properties>
    <java.version>17</java.version>
    <springdoc.version>2.8.17</springdoc.version>
    <rome.version>2.1.0</rome.version>
</properties>
```

Add under dependencies:

```xml
<dependency>
    <groupId>com.rometools</groupId>
    <artifactId>rome</artifactId>
    <version>${rome.version}</version>
</dependency>
```

- [ ] **Step 2: Implement parser**

Create `backend/src/main/java/com/pulsebrief/ingestion/provider/RssFeedParser.java`:

```java
package com.pulsebrief.ingestion.provider;

import com.pulsebrief.ingestion.service.IngestionRequest;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import java.io.StringReader;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import org.jdom2.input.SAXBuilder;
import org.springframework.stereotype.Component;

@Component
public class RssFeedParser {
    public List<RawNewsPayload> parse(String feedXml, IngestionRequest request) {
        try {
            SyndFeed feed = new SyndFeedInput().build(new SAXBuilder().build(new StringReader(feedXml)));
            String sourceName = cleanText(feed.getTitle());
            return feed.getEntries().stream()
                    .map(entry -> toPayload(sourceName, entry, request))
                    .flatMap(List::stream)
                    .filter(payload -> matchesKeywords(payload, request.keywords()))
                    .limit(request.pageSizeOrDefault())
                    .toList();
        } catch (Exception ex) {
            throw new IllegalArgumentException("Failed to parse RSS feed", ex);
        }
    }

    private List<RawNewsPayload> toPayload(String feedTitle, SyndEntry entry, IngestionRequest request) {
        String title = cleanText(entry.getTitle());
        String originalUrl = cleanText(entry.getLink());
        if (title == null || originalUrl == null) {
            return List.of();
        }
        String providerItemId = cleanText(entry.getUri());
        if (providerItemId == null) {
            providerItemId = originalUrl;
        }
        String summary = entry.getDescription() == null ? null : cleanText(entry.getDescription().getValue());
        OffsetDateTime publishedAt = entry.getPublishedDate() == null
                ? null
                : entry.getPublishedDate().toInstant().atOffset(ZoneOffset.UTC);
        return List.of(new RawNewsPayload(
                providerItemId,
                title,
                summary,
                feedTitle,
                originalUrl,
                null,
                publishedAt,
                normalizeLanguage(request.language()),
                normalizeCountry(request.country()),
                "{\"providerItemId\":\"" + escapeJson(providerItemId) + "\",\"source\":\"RSS\"}"
        ));
    }

    private boolean matchesKeywords(RawNewsPayload payload, List<String> keywords) {
        if (keywords == null || keywords.isEmpty()) {
            return true;
        }
        String searchable = ((payload.title() == null ? "" : payload.title()) + " "
                + (payload.summary() == null ? "" : payload.summary())).toLowerCase(Locale.ROOT);
        return keywords.stream()
                .map(keyword -> keyword == null ? "" : keyword.toLowerCase(Locale.ROOT).trim())
                .filter(keyword -> !keyword.isBlank())
                .anyMatch(searchable::contains);
    }

    private String cleanText(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = value.replaceAll("<[^>]+>", " ")
                .replaceAll("\\s+", " ")
                .trim();
        return cleaned.isBlank() ? null : cleaned;
    }

    private String normalizeLanguage(String language) {
        if (language == null || language.isBlank()) {
            return null;
        }
        return language.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeCountry(String country) {
        if (country == null || country.isBlank()) {
            return null;
        }
        return country.trim().toUpperCase(Locale.ROOT);
    }

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
```

- [ ] **Step 3: Run RSS parser tests**

Run:

```powershell
cd backend
.\mvnw.cmd -Dtest=RssNewsIngestionProviderTest test
```

Expected: tests pass or fail only for exact mapping adjustments. If mapping fails, adjust `RssFeedParser` only.

## Task 3: Add Provider And Context Registration

**Files:**
- Create: `backend/src/main/java/com/pulsebrief/ingestion/provider/RssFeedClient.java`
- Create: `backend/src/main/java/com/pulsebrief/ingestion/provider/RssNewsIngestionProvider.java`
- Modify: `backend/src/test/java/com/pulsebrief/ingestion/provider/NewsIngestionProviderContextTest.java`

- [ ] **Step 1: Write failing context expectation**

Modify `NewsIngestionProviderContextTest`:

```java
@Test
void registersFixtureAndRssProvidersAsSpringBeans() {
    assertThat(providers)
            .extracting(NewsIngestionProvider::providerType)
            .contains("FIXTURE", "RSS");
}
```

Run:

```powershell
cd backend
.\mvnw.cmd -Dtest=NewsIngestionProviderContextTest test
```

Expected: FAIL because `RSS` provider is not registered.

- [ ] **Step 2: Add feed client**

Create `backend/src/main/java/com/pulsebrief/ingestion/provider/RssFeedClient.java`:

```java
package com.pulsebrief.ingestion.provider;

import java.net.URI;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class RssFeedClient {
    private final RestClient restClient;

    public RssFeedClient(RestClient.Builder builder) {
        this.restClient = builder.build();
    }

    public String fetch(String feedUrl) {
        return restClient.get()
                .uri(URI.create(feedUrl))
                .retrieve()
                .body(String.class);
    }
}
```

- [ ] **Step 3: Add RSS provider**

Create `backend/src/main/java/com/pulsebrief/ingestion/provider/RssNewsIngestionProvider.java`:

```java
package com.pulsebrief.ingestion.provider;

import com.pulsebrief.ingestion.service.IngestionRequest;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class RssNewsIngestionProvider implements NewsIngestionProvider {
    private final RssFeedClient feedClient;
    private final RssFeedParser parser;

    public RssNewsIngestionProvider(RssFeedClient feedClient, RssFeedParser parser) {
        this.feedClient = feedClient;
        this.parser = parser;
    }

    @Override
    public String providerType() {
        return "RSS";
    }

    @Override
    public List<RawNewsPayload> fetch(IngestionRequest request) {
        throw new UnsupportedOperationException("RSS feed URL selection is implemented in the live smoke task");
    }

    public List<RawNewsPayload> fetchFeed(String feedUrl, IngestionRequest request) {
        return parser.parse(feedClient.fetch(feedUrl), request);
    }
}
```

- [ ] **Step 4: Run context test**

Run:

```powershell
cd backend
.\mvnw.cmd -Dtest=NewsIngestionProviderContextTest test
```

Expected: PASS with both `FIXTURE` and `RSS` providers present.

## Task 4: Add Guarded Live Smoke Test

**Files:**
- Create: `backend/src/test/java/com/pulsebrief/ingestion/provider/RssLiveSmokeTest.java`

- [ ] **Step 1: Write disabled-by-default live smoke test**

Create `backend/src/test/java/com/pulsebrief/ingestion/provider/RssLiveSmokeTest.java`:

```java
package com.pulsebrief.ingestion.provider;

import com.pulsebrief.ingestion.service.IngestionRequest;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class RssLiveSmokeTest {
    @Autowired
    private RssNewsIngestionProvider provider;

    @Test
    @EnabledIfEnvironmentVariable(named = "PULSEBRIEF_RSS_LIVE_TEST_ENABLED", matches = "true")
    void fetchesRealPublicRssFeedWhenExplicitlyEnabled() {
        String feedUrl = System.getenv("PULSEBRIEF_RSS_LIVE_TEST_URL");
        assertThat(feedUrl).isNotBlank();

        List<RawNewsPayload> payloads = provider.fetchFeed(
                feedUrl,
                new IngestionRequest("rss-live-smoke", List.of(), "en", "US", "global", 5)
        );

        assertThat(payloads).isNotEmpty();
        assertThat(payloads).hasSizeLessThanOrEqualTo(5);
        assertThat(payloads.get(0).title()).isNotBlank();
        assertThat(payloads.get(0).originalUrl()).isNotBlank();
    }
}
```

- [ ] **Step 2: Verify live smoke is skipped by default**

Run:

```powershell
cd backend
.\mvnw.cmd -Dtest=RssLiveSmokeTest test
```

Expected: test class runs with 1 skipped test or no enabled test failure; no external network request is made.

- [ ] **Step 3: Run manual live smoke**

Run only after confirming the feed URL is public and does not require login:

```powershell
$env:PULSEBRIEF_RSS_LIVE_TEST_ENABLED='true'
$env:PULSEBRIEF_RSS_LIVE_TEST_URL='https://www.investing.com/rss/news.rss'
cd backend
.\mvnw.cmd -Dtest=RssLiveSmokeTest test
Remove-Item Env:\PULSEBRIEF_RSS_LIVE_TEST_ENABLED
Remove-Item Env:\PULSEBRIEF_RSS_LIVE_TEST_URL
```

Expected: PASS with at least one parsed payload and at most five payloads. If the selected feed blocks requests, use another public RSS source and record the final URL in the stage handoff.

## Task 5: Documentation And Regression

**Files:**
- Modify: `docs/测试方案.md`
- Modify: `docs/下一阶段任务清单.md`

- [ ] **Step 1: Update test plan**

Add a short Stage 23 RSS Provider section to `docs/测试方案.md`:

```markdown
## 真实 RSS Provider V1 测试

阶段 23 自动化测试只使用本地 RSS/Atom fixture，不访问真实外网：

```powershell
cd backend
.\mvnw.cmd -Dtest=RssNewsIngestionProviderTest,NewsIngestionProviderContextTest test
```

真实外网采集验证只作为本地手动 live smoke，必须显式开启：

```powershell
$env:PULSEBRIEF_RSS_LIVE_TEST_ENABLED='true'
$env:PULSEBRIEF_RSS_LIVE_TEST_URL='<public-rss-url>'
cd backend
.\mvnw.cmd -Dtest=RssLiveSmokeTest test
```

live smoke 不进入 CI，不提交 `.env.local`、采集输出文件、PDF 或数据库 dump。
```

- [ ] **Step 2: Update task list**

Add Stage 23 status to `docs/下一阶段任务清单.md`:

```markdown
## 阶段 23：真实 RSS Provider V1

- [x] 新增真实 RSS Provider V1 设计：`docs/真实RSSProviderV1设计.md`。
- [x] 新增 RSS/Atom fixture 自动化测试。
- [x] 新增 `RSS` Provider Spring Bean。
- [x] 新增显式开关保护的真实外网 live smoke。
- [ ] 执行并记录本地 live smoke 结果。

阶段结果：RSS Provider 已具备元数据采集能力，CI 继续使用本地 fixture，真实外网采集验证通过显式 live smoke 执行。
```

- [ ] **Step 3: Run focused regression**

Run:

```powershell
cd backend
.\mvnw.cmd -Dtest=RssNewsIngestionProviderTest,NewsIngestionProviderContextTest,RssLiveSmokeTest test
```

Expected: RSS parser and context tests pass; live smoke is skipped unless explicitly enabled.

- [ ] **Step 4: Run full backend tests**

Run:

```powershell
cd backend
.\mvnw.cmd test
```

Expected: all backend tests pass. If local MySQL is unstable, record the failure and use CI after push as final verification.

- [ ] **Step 5: Commit implementation**

Run:

```powershell
git add backend/pom.xml backend/src/main/java/com/pulsebrief/ingestion/provider backend/src/test/java/com/pulsebrief/ingestion/provider backend/src/test/resources/ingestion/rss docs/测试方案.md docs/下一阶段任务清单.md
git commit -m "feat: add rss metadata provider"
```

Expected: one implementation commit after the design commit.

## Self-Review

- Spec coverage: The plan covers fixture-only automated RSS/Atom parsing, provider registration, explicit live smoke, and docs updates.
- Placeholder scan: No `TBD`, `TODO`, or "fill in later" placeholders remain.
- Type consistency: Plan uses existing `NewsIngestionProvider`, `RawNewsPayload`, and `IngestionRequest` names.
