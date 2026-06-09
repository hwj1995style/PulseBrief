package com.pulsebrief.ingestion;

import com.pulsebrief.ingestion.domain.CandidateArticle;
import com.pulsebrief.ingestion.domain.ReportAsset;
import com.pulsebrief.ingestion.provider.RawNewsPayload;
import com.pulsebrief.ingestion.repository.CandidateArticleRepository;
import com.pulsebrief.ingestion.repository.ReportAssetRepository;
import com.pulsebrief.ingestion.service.CandidateArticleGenerationService;
import com.pulsebrief.ingestion.service.ReportAssetRegistrationService;
import com.pulsebrief.ingestion.service.RawNewsIngestionService;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ReportAssetRegistrationServiceTest {
    @Autowired
    private RawNewsIngestionService ingestionService;

    @Autowired
    private CandidateArticleGenerationService candidateGenerationService;

    @Autowired
    private CandidateArticleRepository candidateArticleRepository;

    @Autowired
    private ReportAssetRegistrationService reportAssetRegistrationService;

    @Autowired
    private ReportAssetRepository reportAssetRepository;

    @Test
    void registersPdfMetadataForCandidateWithoutDownloadingFile() {
        String uniquePath = UUID.randomUUID().toString();
        CandidateArticle candidate = createCandidate(uniquePath);
        String fileHash = "sha256-" + uniquePath;

        ReportAsset asset = reportAssetRegistrationService.registerPdfMetadata(
                candidate.getId(),
                "fixture-report",
                "AI infrastructure public report " + uniquePath,
                "https://example.com/reports/" + uniquePath + ".pdf",
                "ai-infrastructure-" + uniquePath + ".pdf",
                128_000L,
                fileHash,
                "PDF_ALLOWED"
        );

        assertThat(asset.getCandidateArticle().getId()).isEqualTo(candidate.getId());
        assertThat(asset.getAssetStatus()).isEqualTo("PENDING_REVIEW");
        assertThat(asset.getLicensePolicy()).isEqualTo("PDF_ALLOWED");
        assertThat(asset.getFileHash()).isEqualTo(fileHash);
        assertThat(reportAssetRepository.countByFileHash(fileHash)).isEqualTo(1);
    }

    @Test
    void skipsDuplicatePdfMetadataByFileHash() {
        String uniquePath = UUID.randomUUID().toString();
        CandidateArticle candidate = createCandidate(uniquePath);
        String fileHash = "sha256-duplicate-" + uniquePath;

        ReportAsset first = reportAssetRegistrationService.registerPdfMetadata(
                candidate.getId(),
                "fixture-report",
                "First public report " + uniquePath,
                "https://example.com/reports/" + uniquePath + "-one.pdf",
                "report-one.pdf",
                64_000L,
                fileHash,
                "PDF_ALLOWED"
        );
        ReportAsset second = reportAssetRegistrationService.registerPdfMetadata(
                candidate.getId(),
                "fixture-report",
                "Second public report " + uniquePath,
                "https://mirror.example.com/reports/" + uniquePath + "-two.pdf",
                "report-two.pdf",
                64_000L,
                fileHash,
                "PDF_ALLOWED"
        );

        assertThat(second.getId()).isEqualTo(first.getId());
        assertThat(reportAssetRepository.countByFileHash(fileHash)).isEqualTo(1);
    }

    private CandidateArticle createCandidate(String uniquePath) {
        String sourceCode = "report-asset-" + uniquePath;
        String title = "Authorized PDF report candidate " + uniquePath;
        ingestionService.ingest(
                sourceCode,
                "MANUAL",
                List.of(new RawNewsPayload(
                        "report-candidate-" + uniquePath,
                        title,
                        "A public report is available for review.",
                        "Example Reports",
                        "https://example.com/report-candidate/" + uniquePath,
                        null,
                        OffsetDateTime.now().minusHours(2),
                        "en",
                        "US",
                        "{\"id\":\"report-candidate-" + uniquePath + "\"}"
                ))
        );
        candidateGenerationService.generatePendingCandidates(sourceCode, 10);
        return candidateArticleRepository.findByTitle(title).orElseThrow();
    }
}
