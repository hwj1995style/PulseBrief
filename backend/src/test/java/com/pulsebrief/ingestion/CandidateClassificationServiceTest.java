package com.pulsebrief.ingestion;

import com.pulsebrief.ingestion.domain.NewsIngestionSource;
import com.pulsebrief.ingestion.domain.RawNewsItem;
import com.pulsebrief.ingestion.provider.RawNewsPayload;
import com.pulsebrief.ingestion.repository.NewsIngestionSourceRepository;
import com.pulsebrief.ingestion.service.CandidateClassificationService;
import com.pulsebrief.ingestion.service.CandidateClassificationProvider;
import com.pulsebrief.ingestion.service.ClassificationDecision;
import java.util.List;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CandidateClassificationServiceTest {
    private final NewsIngestionSourceRepository sourceRepository = mock(NewsIngestionSourceRepository.class);
    private final CandidateClassificationService service = new CandidateClassificationService(sourceRepository, java.util.List.of());

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

    @Test
    void usesModelForUnmatchedMetadata() {
        CandidateClassificationProvider provider = mock(CandidateClassificationProvider.class);
        RawNewsItem item = rawItem("missing", "Quarterly business update", "New operating targets");
        when(provider.classify(item)).thenReturn(Optional.of(
                new ClassificationDecision("company", 0.81, "MODEL_DEEPSEEK:COMPANY_RESULTS")
        ));

        ClassificationDecision decision = new CandidateClassificationService(sourceRepository, List.of(provider))
                .classify(item);

        assertThat(decision.suggestedCategoryCode()).isEqualTo("company");
        assertThat(decision.confidence()).isEqualTo(0.81);
        assertThat(decision.matchedRule()).isEqualTo("MODEL_DEEPSEEK:COMPANY_RESULTS");
    }

    @Test
    void keepsKeywordRulesAheadOfModel() {
        CandidateClassificationProvider provider = mock(CandidateClassificationProvider.class);
        RawNewsItem item = rawItem("missing", "Nvidia expands generative AI platform", "Enterprise adoption");

        ClassificationDecision decision = new CandidateClassificationService(sourceRepository, List.of(provider))
                .classify(item);

        assertThat(decision.suggestedCategoryCode()).isEqualTo("ai");
        assertThat(decision.matchedRule()).startsWith("KEYWORD_AI:");
        verifyNoInteractions(provider);
    }

    @Test
    void marksLowConfidenceModelFallback() {
        CandidateClassificationProvider provider = mock(CandidateClassificationProvider.class);
        RawNewsItem item = rawItem("missing", "Quarterly business update", "New operating targets");
        when(provider.classify(item)).thenReturn(Optional.empty());

        ClassificationDecision decision = new CandidateClassificationService(sourceRepository, List.of(provider))
                .classify(item);

        assertThat(decision.suggestedCategoryCode()).isEqualTo("global");
        assertThat(decision.matchedRule()).isEqualTo("MODEL_FALLBACK_GLOBAL");
    }

    @Test
    void fallsBackSafelyWhenModelFails() {
        CandidateClassificationProvider provider = mock(CandidateClassificationProvider.class);
        NewsIngestionSource source = NewsIngestionSource.fixture("model-fallback", "Fallback source", "METADATA_ONLY", 24);
        RawNewsItem item = rawItem("model-fallback", "Quarterly business update", "New operating targets");
        when(provider.providerType()).thenReturn("DEEPSEEK");
        when(provider.classify(item)).thenThrow(new IllegalStateException("provider unavailable"));
        when(sourceRepository.findByCode("model-fallback")).thenReturn(Optional.of(source));

        ClassificationDecision decision = new CandidateClassificationService(sourceRepository, List.of(provider))
                .classify(item);

        assertThat(decision.suggestedCategoryCode()).isEqualTo("global");
        assertThat(decision.matchedRule()).isEqualTo("MODEL_FALLBACK_SOURCE_DEFAULT");
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
