package com.pulsebrief.ingestion;

import com.pulsebrief.ingestion.domain.NewsIngestionJob;
import com.pulsebrief.ingestion.provider.RawNewsPayload;
import com.pulsebrief.ingestion.repository.NewsIngestionJobRepository;
import com.pulsebrief.ingestion.repository.RawNewsItemRepository;
import com.pulsebrief.ingestion.service.IngestionResult;
import com.pulsebrief.ingestion.service.RawNewsIngestionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class RawNewsIngestionServiceTest {
    @Autowired
    private RawNewsIngestionService ingestionService;

    @Autowired
    private RawNewsItemRepository rawNewsItemRepository;

    @Autowired
    private NewsIngestionJobRepository jobRepository;

    @Test
    void storesNewRawItemsAndSkipsDuplicateOriginalUrls() {
        String uniquePath = UUID.randomUUID().toString();
        String originalUrl = "https://example.com/ingestion/" + uniquePath;
        String title = "AI infrastructure keeps expanding " + uniquePath;

        IngestionResult result = ingestionService.ingest(
                "fixture-global",
                "MANUAL",
                List.of(
                        payload("one-" + uniquePath, title, originalUrl),
                        payload("duplicate-" + uniquePath, title, originalUrl)
                )
        );

        assertThat(result.fetchedCount()).isEqualTo(2);
        assertThat(result.newCount()).isEqualTo(1);
        assertThat(result.duplicateCount()).isEqualTo(1);
        assertThat(rawNewsItemRepository.countByOriginalUrl(originalUrl)).isEqualTo(1);

        NewsIngestionJob job = jobRepository.findById(result.jobId()).orElseThrow();
        assertThat(job.getJobStatus()).isEqualTo("SUCCESS");
        assertThat(job.getFetchedCount()).isEqualTo(2);
        assertThat(job.getNewCount()).isEqualTo(1);
        assertThat(job.getDuplicateCount()).isEqualTo(1);
        assertThat(job.getFinishedAt()).isNotNull();
    }

    @Test
    void skipsDuplicateContentHashesAcrossDifferentUrls() {
        String uniquePath = UUID.randomUUID().toString();
        String firstUrl = "https://example.com/ingestion/" + uniquePath + "/first";
        String secondUrl = "https://mirror.example.com/ingestion/" + uniquePath + "/second";
        String title = "Central bank officials keep cautious tone " + uniquePath;

        IngestionResult result = ingestionService.ingest(
                "fixture-global",
                "MANUAL",
                List.of(
                        payload("content-one-" + uniquePath, title, firstUrl),
                        payload("content-two-" + uniquePath, " " + title + "! ", secondUrl)
                )
        );

        assertThat(result.fetchedCount()).isEqualTo(2);
        assertThat(result.newCount()).isEqualTo(1);
        assertThat(result.duplicateCount()).isEqualTo(1);
        assertThat(rawNewsItemRepository.countByOriginalUrl(firstUrl)).isEqualTo(1);
        assertThat(rawNewsItemRepository.countByOriginalUrl(secondUrl)).isZero();
    }

    private RawNewsPayload payload(String providerItemId, String title, String originalUrl) {
        return new RawNewsPayload(
                providerItemId,
                title,
                "Fixture summary",
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
