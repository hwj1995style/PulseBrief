package com.pulsebrief.ingestion;

import com.pulsebrief.article.service.ArticleService;
import com.pulsebrief.ingestion.domain.CandidateArticle;
import com.pulsebrief.ingestion.provider.RawNewsPayload;
import com.pulsebrief.ingestion.repository.CandidateArticleRepository;
import com.pulsebrief.ingestion.service.CandidateArticleGenerationResult;
import com.pulsebrief.ingestion.service.CandidateArticleGenerationService;
import com.pulsebrief.ingestion.service.RawNewsIngestionService;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class CandidateArticleGenerationServiceTest {
    @Autowired
    private RawNewsIngestionService ingestionService;

    @Autowired
    private CandidateArticleGenerationService candidateGenerationService;

    @Autowired
    private CandidateArticleRepository candidateArticleRepository;

    @Autowired
    private ArticleService articleService;

    @Test
    void generatesPendingCandidateFromNewRawItem() {
        String uniquePath = UUID.randomUUID().toString();
        String sourceCode = "fixture-" + uniquePath;
        ingestionService.ingest(
                sourceCode,
                "MANUAL",
                List.of(payload(
                        "candidate-" + uniquePath,
                        "Nvidia AI chip demand accelerates " + uniquePath,
                        "Enterprise AI infrastructure demand remains strong.",
                        "https://example.com/candidate/" + uniquePath
                ))
        );

        CandidateArticleGenerationResult result = candidateGenerationService.generatePendingCandidates(sourceCode, 10);

        assertThat(result.generatedCount()).isEqualTo(1);
        CandidateArticle candidate = candidateArticleRepository
                .findByTitle("Nvidia AI chip demand accelerates " + uniquePath)
                .orElseThrow();
        assertThat(candidate.getStatus()).isEqualTo("PENDING_REVIEW");
        assertThat(candidate.getCategoryCode()).isEqualTo("ai");
        assertThat(candidate.getSuggestedCategoryCode()).isEqualTo("ai");
        assertThat(candidate.getClassificationConfidence()).isEqualTo(0.94);
        assertThat(candidate.getClassificationRule()).startsWith("KEYWORD_AI:");
        assertThat(candidate.getSourceName()).isEqualTo("Example Markets");
        assertThat(candidate.getOriginalUrl()).isEqualTo("https://example.com/candidate/" + uniquePath);
        assertThat(candidate.getRawNewsItem().getItemStatus()).isEqualTo("CANDIDATE");
    }

    @Test
    void skipsRawItemsThatAlreadyHaveCandidates() {
        String uniquePath = UUID.randomUUID().toString();
        String sourceCode = "fixture-" + uniquePath;
        ingestionService.ingest(
                sourceCode,
                "MANUAL",
                List.of(payload(
                        "candidate-repeat-" + uniquePath,
                        "Goldman public view on AI capex " + uniquePath,
                        "Public investment bank commentary highlights AI capex.",
                        "https://example.com/candidate-repeat/" + uniquePath
                ))
        );

        CandidateArticleGenerationResult first = candidateGenerationService.generatePendingCandidates(sourceCode, 10);
        CandidateArticleGenerationResult second = candidateGenerationService.generatePendingCandidates(sourceCode, 10);

        assertThat(first.generatedCount()).isEqualTo(1);
        assertThat(second.generatedCount()).isZero();
        assertThat(candidateArticleRepository.countByOriginalUrl("https://example.com/candidate-repeat/" + uniquePath))
                .isEqualTo(1);
    }

    @Test
    void pendingCandidatesDoNotAppearInUserArticleList() {
        String uniquePath = UUID.randomUUID().toString();
        String sourceCode = "fixture-" + uniquePath;
        String title = "Unreviewed candidate should stay hidden " + uniquePath;
        ingestionService.ingest(
                sourceCode,
                "MANUAL",
                List.of(payload(
                        "candidate-hidden-" + uniquePath,
                        title,
                        "This item is not reviewed yet.",
                        "https://example.com/candidate-hidden/" + uniquePath
                ))
        );
        candidateGenerationService.generatePendingCandidates(sourceCode, 10);

        assertThat(articleService.listArticles("all", 1, 50))
                .noneMatch(article -> article.title().equals(title));
    }

    private RawNewsPayload payload(String providerItemId, String title, String summary, String originalUrl) {
        return new RawNewsPayload(
                providerItemId,
                title,
                summary,
                "Example Markets",
                originalUrl,
                null,
                OffsetDateTime.parse("2026-06-09T09:00:00+08:00"),
                "en",
                "US",
                "{\"id\":\"" + providerItemId + "\"}"
        );
    }
}
