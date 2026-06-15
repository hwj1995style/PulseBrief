package com.pulsebrief.ingestion.provider;

import com.pulsebrief.ingestion.service.IngestionRequest;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

@EnabledIfEnvironmentVariable(named = "PULSEBRIEF_RSS_LIVE_TEST_ENABLED", matches = "true")
class RssNewsIngestionProviderLiveTest {
    private static final String DEFAULT_LIVE_FEED_URL = "https://feeds.bbci.co.uk/news/world/rss.xml";

    @Test
    void fetchesRealExternalRssMetadata() {
        String feedUrl = liveFeedUrl();
        RssNewsIngestionProvider provider = new RssNewsIngestionProvider(
                new HttpRssFeedClient(RestClient.builder()),
                new RssFeedParser()
        );

        List<RawNewsPayload> payloads = provider.fetchFeed(
                feedUrl,
                new IngestionRequest("rss-live-smoke", List.of(), "en", "GB", "global", 5)
        );

        assertThat(payloads).isNotEmpty();
        assertThat(payloads).hasSizeLessThanOrEqualTo(5);
        assertThat(payloads)
                .allSatisfy(payload -> {
                    assertThat(payload.title()).isNotBlank();
                    assertThat(payload.originalUrl()).startsWith("http");
                    assertThat(payload.sourceName()).isNotBlank();
                    assertThat(payload.rawPayload()).contains("providerItemId");
                });
    }

    private String liveFeedUrl() {
        String configuredUrl = System.getenv("PULSEBRIEF_RSS_LIVE_TEST_URL");
        if (configuredUrl == null || configuredUrl.isBlank()) {
            return DEFAULT_LIVE_FEED_URL;
        }
        return configuredUrl.trim();
    }
}
