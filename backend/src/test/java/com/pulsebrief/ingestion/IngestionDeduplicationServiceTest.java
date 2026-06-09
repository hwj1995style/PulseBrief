package com.pulsebrief.ingestion;

import com.pulsebrief.ingestion.service.IngestionDeduplicationService;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class IngestionDeduplicationServiceTest {
    private final IngestionDeduplicationService deduplicationService = new IngestionDeduplicationService();

    @Test
    void createsStableUrlHash() {
        String first = deduplicationService.urlHash(" HTTPS://Example.com/News?id=1 ");
        String second = deduplicationService.urlHash("https://example.com/news?id=1");

        assertThat(first).isEqualTo(second);
        assertThat(first).hasSize(64);
    }

    @Test
    void createsStableContentHashFromSourceTitleAndPublishDate() {
        String first = deduplicationService.contentHash(
                "Example Markets",
                " AI: Market   Rally! ",
                OffsetDateTime.parse("2026-06-09T09:00:00+08:00")
        );
        String second = deduplicationService.contentHash(
                "example markets",
                "ai market rally",
                OffsetDateTime.parse("2026-06-09T20:00:00+08:00")
        );

        assertThat(first).isEqualTo(second);
        assertThat(first).hasSize(64);
    }
}
