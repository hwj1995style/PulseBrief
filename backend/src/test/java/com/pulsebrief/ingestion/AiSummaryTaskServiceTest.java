package com.pulsebrief.ingestion;

import com.pulsebrief.ingestion.domain.AiSummaryTask;
import com.pulsebrief.ingestion.domain.CandidateArticle;
import com.pulsebrief.ingestion.domain.NewsIngestionSource;
import com.pulsebrief.ingestion.domain.RawNewsContent;
import com.pulsebrief.ingestion.provider.RawNewsPayload;
import com.pulsebrief.ingestion.repository.CandidateArticleRepository;
import com.pulsebrief.ingestion.repository.NewsIngestionSourceRepository;
import com.pulsebrief.ingestion.repository.RawNewsContentRepository;
import com.pulsebrief.ingestion.service.AiSummaryTaskService;
import com.pulsebrief.ingestion.service.CandidateArticleGenerationService;
import com.pulsebrief.ingestion.service.RawNewsIngestionService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class AiSummaryTaskServiceTest {
    @Autowired
    private RawNewsIngestionService ingestionService;

    @Autowired
    private CandidateArticleGenerationService candidateGenerationService;

    @Autowired
    private CandidateArticleRepository candidateArticleRepository;

    @Autowired
    private NewsIngestionSourceRepository sourceRepository;

    @Autowired
    private RawNewsContentRepository rawNewsContentRepository;

    @Autowired
    private AiSummaryTaskService aiSummaryTaskService;

    @Test
    void generatesMockSummaryFromRssSummary() {
        CandidateArticle candidate = createCandidate(
                "rss-summary",
                "AI infrastructure investment remains active and investors are watching capex and power demand.",
                "SUMMARY_ONLY"
        );

        AiSummaryTask task = aiSummaryTaskService.generate(
                candidate.getId(),
                "AUTO",
                "MOCK",
                "candidate-summary-v1"
        );

        assertThat(task.getTaskStatus()).isEqualTo("SUCCESS");
        assertThat(task.getInputSourceType()).isEqualTo("RSS_SUMMARY");
        assertThat(task.getInputHash()).hasSize(64);
        assertThat(task.getGeneratedSummary()).contains(candidate.getTitle());
        assertThat(task.getGeneratedKeyPoints()).contains("Mock AI");
        assertThat(task.getGeneratedImpactAnalysis()).contains("审核");
    }

    @Test
    void prefersAuthorizedContentSnippetOverRssSummary() {
        CandidateArticle candidate = createCandidate(
                "content-snippet",
                "RSS summary should not be the primary input when authorized content exists.",
                "FULLTEXT_ALLOWED"
        );
        RawNewsContent content = rawNewsContentRepository.save(RawNewsContent.success(
                candidate.getRawNewsItem(),
                "SNIPPET",
                "FULLTEXT_ALLOWED",
                "Fixture authorized snippet for AI summary tests",
                "Authorized snippet body for AI summary. Cloud spending, data centers and chips remain the focus.",
                sha256("Authorized snippet body for AI summary. Cloud spending, data centers and chips remain the focus.")
        ));

        AiSummaryTask task = aiSummaryTaskService.generate(
                candidate.getId(),
                "AUTO",
                "MOCK",
                "candidate-summary-v1"
        );

        assertThat(task.getTaskStatus()).isEqualTo("SUCCESS");
        assertThat(task.getInputSourceType()).isEqualTo("CONTENT_SNIPPET");
        assertThat(task.getInputRefId()).isEqualTo(content.getId());
        assertThat(task.getInputPreview()).contains("Authorized snippet body");
    }

    @Test
    void storesSkippedTaskWhenInputIsBlank() {
        CandidateArticle candidate = createCandidate("blank-input", " ", "SUMMARY_ONLY");

        AiSummaryTask task = aiSummaryTaskService.generate(
                candidate.getId(),
                "AUTO",
                "MOCK",
                "candidate-summary-v1"
        );

        assertThat(task.getTaskStatus()).isEqualTo("SKIPPED");
        assertThat(task.getErrorMessage()).contains("AI summary input is empty");
        assertThat(task.getGeneratedSummary()).isNull();
    }

    private CandidateArticle createCandidate(String prefix, String summary, String contentAccessPolicy) {
        String uniquePath = prefix + "-" + UUID.randomUUID();
        String sourceCode = "ai-summary-" + uniquePath;
        String title = "AI summary candidate " + uniquePath;
        sourceRepository.save(NewsIngestionSource.fixture(
                sourceCode,
                "AI summary source " + uniquePath,
                contentAccessPolicy,
                24
        ));
        ingestionService.ingest(
                sourceCode,
                "MANUAL",
                List.of(new RawNewsPayload(
                        "provider-" + uniquePath,
                        title,
                        summary,
                        "AI Summary Fixture",
                        "https://example.com/ai-summary/" + uniquePath,
                        null,
                        OffsetDateTime.now().minusHours(1),
                        "en",
                        "US",
                        "{\"id\":\"" + uniquePath + "\"}"
                ))
        );
        candidateGenerationService.generatePendingCandidates(sourceCode, 1);
        return candidateArticleRepository.findByTitle(title).orElseThrow();
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
