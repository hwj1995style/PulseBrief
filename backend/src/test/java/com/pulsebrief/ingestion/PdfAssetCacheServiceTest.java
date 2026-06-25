package com.pulsebrief.ingestion;

import com.pulsebrief.ingestion.domain.CandidateArticle;
import com.pulsebrief.ingestion.domain.NewsIngestionSource;
import com.pulsebrief.ingestion.domain.ReportAsset;
import com.pulsebrief.ingestion.domain.ReportAssetFile;
import com.pulsebrief.ingestion.provider.RawNewsPayload;
import com.pulsebrief.ingestion.repository.CandidateArticleRepository;
import com.pulsebrief.ingestion.repository.NewsIngestionSourceRepository;
import com.pulsebrief.ingestion.repository.ReportAssetFileRepository;
import com.pulsebrief.ingestion.repository.ReportAssetRepository;
import com.pulsebrief.ingestion.service.CandidateArticleGenerationService;
import com.pulsebrief.ingestion.service.DownloadedPdf;
import com.pulsebrief.ingestion.service.PdfAssetCacheService;
import com.pulsebrief.ingestion.service.PdfDownloadClient;
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
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "pulsebrief.pdf-cache.storage-dir=target/test-pdf-cache",
        "pulsebrief.pdf-cache.enabled=true"
})
class PdfAssetCacheServiceTest {
    private static final byte[] FIXTURE_PDF_BYTES =
            "%PDF-1.4\nfixture public report\n%%EOF".getBytes(StandardCharsets.UTF_8);

    @Autowired
    private RawNewsIngestionService ingestionService;

    @Autowired
    private CandidateArticleGenerationService candidateGenerationService;

    @Autowired
    private CandidateArticleRepository candidateArticleRepository;

    @Autowired
    private NewsIngestionSourceRepository sourceRepository;

    @Autowired
    private ReportAssetRepository reportAssetRepository;

    @Autowired
    private ReportAssetFileRepository reportAssetFileRepository;

    @Autowired
    private PdfAssetCacheService pdfAssetCacheService;

    @Test
    void cachesAuthorizedPdfAndStoresPhysicalFileOnce() {
        CandidateArticle candidate = createCandidate("pdf-cache-success", "PDF_ALLOWED");
        ReportAsset asset = createReportAsset(candidate, "legacy-hash-" + UUID.randomUUID());

        ReportAsset cachedAsset = pdfAssetCacheService.cacheAsset(candidate.getId(), asset.getId());

        assertThat(cachedAsset.getCacheStatus()).isEqualTo("SUCCESS");
        assertThat(cachedAsset.getCacheErrorMessage()).isNull();
        assertThat(cachedAsset.getLicenseNote()).contains("Fixture source");
        assertThat(cachedAsset.getAssetFile()).isNotNull();

        ReportAssetFile file = reportAssetFileRepository.findByFileHash(sha256(FIXTURE_PDF_BYTES)).orElseThrow();
        assertThat(cachedAsset.getAssetFile().getId()).isEqualTo(file.getId());
        assertThat(file.getMimeType()).isEqualTo("application/pdf");
        assertThat(file.getFileSizeBytes()).isEqualTo((long) FIXTURE_PDF_BYTES.length);
    }

    @Test
    void skipsPdfCacheWhenSourceIsNotAuthorized() {
        CandidateArticle candidate = createCandidate("pdf-cache-skip", "SUMMARY_ONLY");
        ReportAsset asset = createReportAsset(candidate, "legacy-skip-" + UUID.randomUUID());

        ReportAsset cachedAsset = pdfAssetCacheService.cacheAsset(candidate.getId(), asset.getId());

        assertThat(cachedAsset.getCacheStatus()).isEqualTo("SKIPPED");
        assertThat(cachedAsset.getCacheErrorMessage()).contains("not authorized");
        assertThat(cachedAsset.getAssetFile()).isNull();
    }

    @Test
    void reusesCachedFileForSamePdfHashAcrossCandidateAssets() {
        CandidateArticle firstCandidate = createCandidate("pdf-cache-reuse-one", "PDF_ALLOWED");
        CandidateArticle secondCandidate = createCandidate("pdf-cache-reuse-two", "PDF_ALLOWED");
        ReportAsset firstAsset = createReportAsset(firstCandidate, "legacy-reuse-one-" + UUID.randomUUID());
        ReportAsset secondAsset = createReportAsset(secondCandidate, "legacy-reuse-two-" + UUID.randomUUID());

        ReportAsset firstCachedAsset = pdfAssetCacheService.cacheAsset(firstCandidate.getId(), firstAsset.getId());
        ReportAsset secondCachedAsset = pdfAssetCacheService.cacheAsset(secondCandidate.getId(), secondAsset.getId());

        assertThat(firstCachedAsset.getCacheStatus()).isEqualTo("SUCCESS");
        assertThat(secondCachedAsset.getCacheStatus()).isEqualTo("SUCCESS");
        assertThat(firstCachedAsset.getAssetFile().getId()).isEqualTo(secondCachedAsset.getAssetFile().getId());
        assertThat(reportAssetFileRepository.findByFileHash(sha256(FIXTURE_PDF_BYTES))).isPresent();
    }

    private CandidateArticle createCandidate(String prefix, String contentAccessPolicy) {
        String uniquePath = prefix + "-" + UUID.randomUUID();
        String sourceCode = "fixture-" + uniquePath;
        String title = "PDF cache candidate " + uniquePath;
        sourceRepository.save(NewsIngestionSource.fixture(
                sourceCode,
                "PDF cache source " + uniquePath,
                contentAccessPolicy,
                24
        ));
        ingestionService.ingest(
                sourceCode,
                "MANUAL",
                List.of(new RawNewsPayload(
                        "provider-" + uniquePath,
                        title,
                        "Public report metadata for PDF cache testing.",
                        "Example Reports",
                        "https://example.com/pdf-cache/" + uniquePath,
                        null,
                        OffsetDateTime.now().minusHours(2),
                        "en",
                        "US",
                        "{\"id\":\"" + uniquePath + "\"}"
                ))
        );
        candidateGenerationService.generatePendingCandidates(sourceCode, 10);
        return candidateArticleRepository.findByTitle(title).orElseThrow();
    }

    private ReportAsset createReportAsset(CandidateArticle candidate, String legacyHash) {
        return reportAssetRepository.save(new ReportAsset(
                candidate,
                candidate.getRawNewsItem().getSourceCode(),
                "Authorized public PDF",
                "https://example.com/reports/" + legacyHash + ".pdf",
                "fixture-report.pdf",
                null,
                legacyHash,
                "PDF_ALLOWED"
        ));
    }

    private String sha256(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(bytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    @TestConfiguration
    static class FixturePdfClientConfig {
        @Bean
        @Primary
        PdfDownloadClient fixturePdfDownloadClient() {
            return url -> new DownloadedPdf("fixture-report.pdf", "application/pdf", FIXTURE_PDF_BYTES);
        }
    }
}
