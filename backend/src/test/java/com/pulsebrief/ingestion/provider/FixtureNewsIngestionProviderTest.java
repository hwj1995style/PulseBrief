package com.pulsebrief.ingestion.provider;

import com.pulsebrief.ingestion.service.IngestionRequest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FixtureNewsIngestionProviderTest {
    private final FixtureNewsIngestionProvider provider = new FixtureNewsIngestionProvider();

    @Test
    void mapsFixtureItemsIntoRawPayloadsWithoutNetwork() {
        List<RawNewsPayload> payloads = provider.fetch(
                new IngestionRequest("fixture-global", List.of(), "en", "US", "global", 10)
        );

        assertThat(provider.providerType()).isEqualTo("FIXTURE");
        assertThat(payloads).hasSize(3);
        assertThat(payloads.get(0).providerItemId()).isEqualTo("fixture-ai-infra");
        assertThat(payloads.get(0).title()).isEqualTo("AI infrastructure investment remains resilient");
        assertThat(payloads.get(0).sourceName()).isEqualTo("Example Markets");
        assertThat(payloads.get(0).originalUrl()).isEqualTo("https://example.com/ai-infra");
        assertThat(payloads.get(0).publishedAt()).isNotNull();
        assertThat(payloads.get(0).rawPayload()).contains("fixture-ai-infra");
    }

    @Test
    void filtersFixtureItemsByKeywordAndPageSize() {
        List<RawNewsPayload> payloads = provider.fetch(
                new IngestionRequest("fixture-global", List.of("central bank"), "en", "US", "global", 1)
        );

        assertThat(payloads).hasSize(1);
        assertThat(payloads.get(0).title()).contains("Central bank");
    }
}
