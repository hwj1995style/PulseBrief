package com.pulsebrief.ingestion.provider;

import com.pulsebrief.ingestion.service.IngestionRequest;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RssNewsIngestionProviderTest {
    private final RssFeedParser parser = new RssFeedParser();

    @Test
    void mapsRssFeedItemsIntoRawPayloadsWithoutNetwork() throws Exception {
        List<RawNewsPayload> payloads = parser.parse(
                fixture("sample-feed.xml"),
                new IngestionRequest("rss-test", List.of(), "en", "US", "global", 10)
        );

        assertThat(payloads).hasSize(3);
        RawNewsPayload first = payloads.get(0);
        assertThat(first.providerItemId()).isEqualTo("rss-ai-chip-001");
        assertThat(first.title()).isEqualTo("AI chip supply expands");
        assertThat(first.summary()).isEqualTo("Chip supply news summary.");
        assertThat(first.sourceName()).isEqualTo("PulseBrief Test RSS");
        assertThat(first.originalUrl()).isEqualTo("https://example.org/news/ai-chip-supply");
        assertThat(first.publishedAt()).isEqualTo(OffsetDateTime.parse("2026-06-15T08:30:00Z"));
        assertThat(first.language()).isEqualTo("en");
        assertThat(first.country()).isEqualTo("US");
        assertThat(first.rawPayload()).contains("rss-ai-chip-001");
    }

    @Test
    void usesLinkAsProviderItemIdWhenGuidIsMissing() throws Exception {
        List<RawNewsPayload> payloads = parser.parse(
                fixture("sample-feed.xml"),
                new IngestionRequest("rss-test", List.of("central bank"), "en", "US", "global", 10)
        );

        assertThat(payloads).hasSize(1);
        assertThat(payloads.get(0).providerItemId())
                .isEqualTo("https://example.org/news/central-bank-comments");
    }

    @Test
    void keepsPayloadWhenPublishedDateCannotBeParsed() throws Exception {
        List<RawNewsPayload> payloads = parser.parse(
                fixture("sample-feed.xml"),
                new IngestionRequest("rss-test", List.of("invalid date"), "en", "US", "global", 10)
        );

        assertThat(payloads).hasSize(1);
        assertThat(payloads.get(0).title()).isEqualTo("Invalid date still maps");
        assertThat(payloads.get(0).publishedAt()).isNull();
    }

    @Test
    void appliesPageSizeAfterSkippingInvalidEntries() throws Exception {
        List<RawNewsPayload> payloads = parser.parse(
                fixture("sample-feed.xml"),
                new IngestionRequest("rss-test", List.of(), "en", "US", "global", 2)
        );

        assertThat(payloads).extracting(RawNewsPayload::title)
                .containsExactly("AI chip supply expands", "Central bank comments steady markets");
    }

    @Test
    void mapsAtomFeedItemsIntoRawPayloadsWithoutNetwork() throws Exception {
        List<RawNewsPayload> payloads = parser.parse(
                fixture("sample-atom-feed.xml"),
                new IngestionRequest("atom-test", List.of(), "en", "US", "global", 10)
        );

        assertThat(payloads).hasSize(1);
        assertThat(payloads.get(0).providerItemId()).isEqualTo("atom-market-001");
        assertThat(payloads.get(0).title()).isEqualTo("Markets digest higher infrastructure spending");
        assertThat(payloads.get(0).originalUrl()).isEqualTo("https://example.org/atom/markets-infra");
        assertThat(payloads.get(0).publishedAt()).isEqualTo(OffsetDateTime.parse("2026-06-15T09:30:00Z"));
    }

    @Test
    void rssProviderDownloadsFeedAndParsesMetadata() throws Exception {
        String fixtureXml = fixture("sample-feed.xml");
        AtomicReference<String> requestedUrl = new AtomicReference<>();
        RssNewsIngestionProvider provider = new RssNewsIngestionProvider(url -> {
            requestedUrl.set(url);
            return fixtureXml;
        }, parser);

        List<RawNewsPayload> payloads = provider.fetchFeed(
                "https://example.org/feed.xml",
                new IngestionRequest("rss-test", List.of(), "en", "US", "global", 1)
        );

        assertThat(requestedUrl.get()).isEqualTo("https://example.org/feed.xml");
        assertThat(payloads).hasSize(1);
        assertThat(payloads.get(0).title()).isEqualTo("AI chip supply expands");
    }

    private String fixture(String name) throws Exception {
        try (InputStream inputStream = getClass().getResourceAsStream("/ingestion/rss/" + name)) {
            assertThat(inputStream).isNotNull();
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
