package com.pulsebrief.ingestion;

import com.pulsebrief.ingestion.domain.NewsIngestionSource;
import com.pulsebrief.ingestion.domain.RawNewsItem;
import com.pulsebrief.ingestion.provider.RawNewsPayload;
import com.pulsebrief.ingestion.repository.NewsIngestionSourceRepository;
import com.pulsebrief.ingestion.service.CandidateClassificationService;
import com.pulsebrief.ingestion.service.ClassificationDecision;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CandidateClassificationServiceTest {
    private final NewsIngestionSourceRepository sourceRepository = mock(NewsIngestionSourceRepository.class);
    private final CandidateClassificationService service = new CandidateClassificationService(sourceRepository);

    @Test
    void classifiesRepresentativeBusinessCategories() {
        assertCategory("Goldman publishes public outlook", "Capital allocation view", "investment_view");
        assertCategory("Nvidia expands generative AI platform", "Enterprise adoption", "ai");
        assertCategory("Cloud software platform launches", "Technology upgrade", "technology");
        assertCategory("Federal Reserve discusses inflation", "Interest rate path", "macro");
        assertCategory("Global stock market advances", "Bond yields ease", "finance");
    }

    @Test
    void fallsBackToSourceDefaultWhenNoKeywordMatches() {
        NewsIngestionSource source = NewsIngestionSource.fixture("source-default", "Default source", "METADATA_ONLY", 24);
        when(sourceRepository.findByCode("source-default")).thenReturn(Optional.of(source));

        ClassificationDecision decision = service.classify(rawItem("source-default", "Morning briefing", "General update"));

        assertThat(decision.suggestedCategoryCode()).isEqualTo("global");
        assertThat(decision.confidence()).isEqualTo(0.55);
        assertThat(decision.matchedRule()).isEqualTo("SOURCE_DEFAULT");
    }

    private void assertCategory(String title, String summary, String expected) {
        ClassificationDecision decision = service.classify(rawItem("missing", title, summary));
        assertThat(decision.suggestedCategoryCode()).isEqualTo(expected);
        assertThat(decision.confidence()).isGreaterThanOrEqualTo(0.80);
    }

    private RawNewsItem rawItem(String sourceCode, String title, String summary) {
        return new RawNewsItem(
                sourceCode,
                new RawNewsPayload(
                        title,
                        title,
                        summary,
                        "Test Source",
                        "https://example.com/" + Math.abs(title.hashCode()),
                        null,
                        OffsetDateTime.parse("2026-07-19T09:00:00+08:00"),
                        "en",
                        "US",
                        "{}"
                ),
                "url-hash-" + Math.abs(title.hashCode()),
                "content-hash-" + Math.abs(summary.hashCode())
        );
    }
}
