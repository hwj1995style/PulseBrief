package com.pulsebrief.ingestion;

import com.pulsebrief.ingestion.domain.NewsIngestionSource;
import com.pulsebrief.ingestion.domain.RawNewsContent;
import com.pulsebrief.ingestion.domain.RawNewsItem;
import com.pulsebrief.ingestion.provider.RawNewsPayload;
import com.pulsebrief.ingestion.repository.NewsIngestionSourceRepository;
import com.pulsebrief.ingestion.repository.RawNewsContentRepository;
import com.pulsebrief.ingestion.repository.RawNewsItemRepository;
import com.pulsebrief.ingestion.service.ContentFetchMode;
import com.pulsebrief.ingestion.service.ContentFetchResult;
import com.pulsebrief.ingestion.service.ContentFetchService;
import com.pulsebrief.ingestion.service.HtmlContentClient;
import com.pulsebrief.ingestion.service.RawNewsIngestionService;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ContentFetchServiceTest {
    @Autowired
    private RawNewsIngestionService ingestionService;

    @Autowired
    private NewsIngestionSourceRepository sourceRepository;

    @Autowired
    private RawNewsItemRepository rawNewsItemRepository;

    @Autowired
    private RawNewsContentRepository rawNewsContentRepository;

    @Autowired
    private ContentFetchService contentFetchService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void skipsSummaryOnlySourceWithoutSavingContent() {
        RawNewsItem rawItem = createRawItem("SUMMARY_ONLY", 24, OffsetDateTime.now().minusHours(2));

        ContentFetchResult result = contentFetchService.fetchRawItem(rawItem.getId(), ContentFetchMode.SNIPPET);

        assertThat(result.fetchStatus()).isEqualTo("SKIPPED");
        assertThat(result.errorMessage()).contains("not authorized");
        assertThat(rawNewsContentRepository.findTopByRawNewsItem_IdOrderByFetchedAtDesc(rawItem.getId()))
                .isEmpty();
    }

    @Test
    void savesSnippetForSnippetAllowedSource() {
        RawNewsItem rawItem = createRawItem("SNIPPET_ALLOWED", 24, OffsetDateTime.now().minusHours(2));

        ContentFetchResult result = contentFetchService.fetchRawItem(rawItem.getId(), ContentFetchMode.SNIPPET);

        assertThat(result.fetchStatus()).isEqualTo("SUCCESS");
        assertThat(result.captureMode()).isEqualTo("SNIPPET");
        assertThat(result.preview()).contains("First paragraph with market context");
        RawNewsContent content = rawNewsContentRepository
                .findTopByRawNewsItem_IdOrderByFetchedAtDesc(rawItem.getId())
                .orElseThrow();
        assertThat(content.getFetchStatus()).isEqualTo("SUCCESS");
        assertThat(content.getContentText()).contains("Second paragraph with policy context");
        assertThat(content.getContentTextHash()).isNotBlank();
    }

    @Test
    void savesFullTextForFullTextAllowedSource() {
        RawNewsItem rawItem = createRawItem("FULLTEXT_ALLOWED", 24, OffsetDateTime.now().minusHours(2));

        ContentFetchResult result = contentFetchService.fetchRawItem(rawItem.getId(), ContentFetchMode.FULLTEXT);

        assertThat(result.fetchStatus()).isEqualTo("SUCCESS");
        assertThat(result.captureMode()).isEqualTo("FULLTEXT");
        assertThat(result.preview()).contains("Third paragraph with earnings context");
        RawNewsContent content = rawNewsContentRepository
                .findTopByRawNewsItem_IdOrderByFetchedAtDesc(rawItem.getId())
                .orElseThrow();
        assertThat(content.getCaptureMode()).isEqualTo("FULLTEXT");
        assertThat(content.getContentText()).contains("Fourth paragraph with risk context");
    }

    @Test
    void skipsWhenLicenseNoteIsMissing() {
        RawNewsItem rawItem = createRawItem("SNIPPET_ALLOWED", 24, OffsetDateTime.now().minusHours(2));
        jdbcTemplate.update(
                "update news_ingestion_source set license_note = '' where code = ?",
                rawItem.getSourceCode()
        );

        ContentFetchResult result = contentFetchService.fetchRawItem(rawItem.getId(), ContentFetchMode.SNIPPET);

        assertThat(result.fetchStatus()).isEqualTo("SKIPPED");
        assertThat(result.errorMessage()).contains("license note");
        assertThat(rawNewsContentRepository.findTopByRawNewsItem_IdOrderByFetchedAtDesc(rawItem.getId()))
                .isEmpty();
    }

    @Test
    void skipsExpiredRawItem() {
        RawNewsItem rawItem = createRawItem("SNIPPET_ALLOWED", 24, OffsetDateTime.now().minusHours(3));
        jdbcTemplate.update(
                "update news_ingestion_source set max_age_hours = 1 where code = ?",
                rawItem.getSourceCode()
        );

        ContentFetchResult result = contentFetchService.fetchRawItem(rawItem.getId(), ContentFetchMode.SNIPPET);

        assertThat(result.fetchStatus()).isEqualTo("SKIPPED");
        assertThat(result.errorMessage()).contains("outside latest window");
        assertThat(rawNewsContentRepository.findTopByRawNewsItem_IdOrderByFetchedAtDesc(rawItem.getId()))
                .isEmpty();
    }

    private RawNewsItem createRawItem(String policy, int maxAgeHours, OffsetDateTime publishedAt) {
        String uniquePath = UUID.randomUUID().toString();
        String sourceCode = "content-fetch-" + uniquePath;
        sourceRepository.save(NewsIngestionSource.fixture(
                sourceCode,
                "Content Fetch " + policy,
                policy,
                maxAgeHours
        ));
        ingestionService.ingest(
                sourceCode,
                "MANUAL",
                List.of(new RawNewsPayload(
                        "content-" + uniquePath,
                        "Authorized content candidate " + uniquePath,
                        "RSS summary for content fetch.",
                        "Example Content Source",
                        "https://example.com/content/" + uniquePath,
                        null,
                        publishedAt,
                        "en",
                        "US",
                        "{\"id\":\"content-" + uniquePath + "\"}"
                ))
        );
        return rawNewsItemRepository
                .findBySourceCodeAndItemStatusOrderByFetchedAtAsc(sourceCode, "NEW", PageRequest.of(0, 1))
                .get(0);
    }

    @TestConfiguration
    static class FixtureContentClientConfig {
        @Bean
        @Primary
        HtmlContentClient fixtureHtmlContentClient() {
            return url -> """
                    <html>
                      <body>
                        <nav>Navigation should be removed</nav>
                        <article>
                          <h1>Fixture article</h1>
                          <p>First paragraph with market context and useful signal.</p>
                          <p>Second paragraph with policy context and source detail.</p>
                          <p>Third paragraph with earnings context and operating detail.</p>
                          <p>Fourth paragraph with risk context and compliance detail.</p>
                        </article>
                      </body>
                    </html>
                    """;
        }
    }
}
