package com.pulsebrief.ingestion;

import com.pulsebrief.ingestion.domain.CandidateArticle;
import com.pulsebrief.ingestion.domain.NewsIngestionSource;
import com.pulsebrief.ingestion.domain.ReportAsset;
import com.pulsebrief.ingestion.domain.ReportAssetFile;
import com.pulsebrief.ingestion.provider.RawNewsPayload;
import com.pulsebrief.ingestion.repository.CandidateArticleRepository;
import com.pulsebrief.ingestion.repository.NewsIngestionSourceRepository;
import com.pulsebrief.ingestion.service.CandidateArticleGenerationService;
import com.pulsebrief.ingestion.service.PdfAssetCacheService;
import com.pulsebrief.ingestion.service.RawNewsIngestionService;
import com.pulsebrief.ingestion.service.ReportAssetRegistrationService;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "pulsebrief.pdf-cache.storage-dir=target/live-pdf-cache",
        "pulsebrief.pdf-cache.enabled=true"
})
class PdfAssetCacheLiveSmokeTest {
    private static final String DEFAULT_PUBLIC_PDF_URL = "https://www.irs.gov/pub/irs-pdf/fw4.pdf";

    @Autowired
    private RawNewsIngestionService ingestionService;

    @Autowired
    private CandidateArticleGenerationService candidateGenerationService;

    @Autowired
    private CandidateArticleRepository candidateArticleRepository;

    @Autowired
    private NewsIngestionSourceRepository sourceRepository;

    @Autowired
    private ReportAssetRegistrationService reportAssetRegistrationService;

    @Autowired
    private PdfAssetCacheService pdfAssetCacheService;

    @Test
    void cachesSinglePublicPdfWhenLiveSmokeIsExplicitlyEnabled() {
        Assumptions.assumeTrue(
                Boolean.parseBoolean(System.getenv("PULSEBRIEF_PDF_LIVE_TEST_ENABLED")),
                "Set PULSEBRIEF_PDF_LIVE_TEST_ENABLED=true to run the live public PDF smoke test"
        );

        String publicPdfUrl = envOrDefault("PULSEBRIEF_PDF_LIVE_TEST_URL", DEFAULT_PUBLIC_PDF_URL);
        String runId = UUID.randomUUID().toString();
        CandidateArticle candidate = createCandidate(runId, publicPdfUrl);
        ReportAsset asset = reportAssetRegistrationService.registerPdfMetadata(
                candidate.getId(),
                candidate.getRawNewsItem().getSourceCode(),
                "IRS public W-4 PDF smoke",
                publicPdfUrl,
                "fw4.pdf",
                null,
                "live-pdf-smoke-" + runId,
                "PDF_ALLOWED"
        );

        ReportAsset cachedAsset = pdfAssetCacheService.cacheAsset(candidate.getId(), asset.getId());

        assertThat(cachedAsset.getCacheStatus()).isEqualTo("SUCCESS");
        assertThat(cachedAsset.getCacheErrorMessage()).isNull();
        assertThat(cachedAsset.getLicenseNote()).contains("Fixture source");
        assertThat(cachedAsset.getAssetFile()).isNotNull();

        ReportAssetFile assetFile = cachedAsset.getAssetFile();
        assertThat(assetFile.getFileHash()).isNotBlank();
        assertThat(assetFile.getMimeType()).containsIgnoringCase("pdf");
        assertThat(assetFile.getFileSizeBytes()).isGreaterThan(0);
        assertThat(assetFile.getStoragePath()).endsWith(assetFile.getFileHash() + ".pdf");

        System.out.printf(
                "PDF live smoke result: source=%s candidateId=%d assetId=%d fileId=%d status=%s bytes=%d mime=%s fileHash=%s url=%s%n",
                candidate.getRawNewsItem().getSourceCode(),
                candidate.getId(),
                cachedAsset.getId(),
                assetFile.getId(),
                cachedAsset.getCacheStatus(),
                assetFile.getFileSizeBytes(),
                assetFile.getMimeType(),
                assetFile.getFileHash(),
                publicPdfUrl
        );
    }

    private CandidateArticle createCandidate(String runId, String publicPdfUrl) {
        String sourceCode = "live-pdf-smoke-" + runId;
        String title = "Live public PDF smoke " + runId;
        sourceRepository.save(NewsIngestionSource.fixture(
                sourceCode,
                "Live public PDF smoke source " + runId,
                "PDF_ALLOWED",
                24
        ));
        ingestionService.ingest(
                sourceCode,
                "MANUAL",
                List.of(new RawNewsPayload(
                        "provider-" + runId,
                        title,
                        "Live smoke candidate used to verify one explicitly authorized public PDF cache path.",
                        "IRS",
                        publicPdfUrl,
                        null,
                        OffsetDateTime.now().minusHours(1),
                        "en",
                        "US",
                        "{\"smoke\":\"" + runId + "\"}"
                ))
        );
        candidateGenerationService.generatePendingCandidates(sourceCode, 1);
        return candidateArticleRepository.findByTitle(title).orElseThrow();
    }

    private String envOrDefault(String name, String defaultValue) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
