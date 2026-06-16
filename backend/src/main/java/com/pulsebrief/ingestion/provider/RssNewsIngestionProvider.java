package com.pulsebrief.ingestion.provider;

import com.pulsebrief.ingestion.domain.NewsIngestionSource;
import com.pulsebrief.ingestion.service.IngestionRequest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class RssNewsIngestionProvider implements NewsIngestionProvider {
    private static final String FEED_URLS_ENV = "PULSEBRIEF_RSS_FEED_URLS";

    private final RssFeedClient feedClient;
    private final RssFeedParser parser;

    public RssNewsIngestionProvider(RssFeedClient feedClient, RssFeedParser parser) {
        this.feedClient = feedClient;
        this.parser = parser;
    }

    @Override
    public String providerType() {
        return "RSS";
    }

    @Override
    public List<RawNewsPayload> fetch(IngestionRequest request) {
        List<String> feedUrls = configuredFeedUrls();
        if (feedUrls.isEmpty()) {
            return List.of();
        }

        int pageSize = request.pageSizeOrDefault();
        List<RawNewsPayload> payloads = new ArrayList<>();
        for (String feedUrl : feedUrls) {
            if (payloads.size() >= pageSize) {
                break;
            }
            IngestionRequest remainingRequest = new IngestionRequest(
                    request.sourceCode(),
                    request.keywords(),
                    request.language(),
                    request.country(),
                    request.market(),
                    pageSize - payloads.size()
            );
            payloads.addAll(fetchFeed(feedUrl, remainingRequest));
        }
        return List.copyOf(payloads);
    }

    @Override
    public List<RawNewsPayload> fetch(NewsIngestionSource source, IngestionRequest request) {
        return fetchFeed(source.getBaseUrl(), request);
    }

    public List<RawNewsPayload> fetchFeed(String feedUrl, IngestionRequest request) {
        if (feedUrl == null || feedUrl.isBlank()) {
            return List.of();
        }
        String feedXml = feedClient.fetch(feedUrl.trim());
        if (feedXml == null || feedXml.isBlank()) {
            return List.of();
        }
        return parser.parse(feedXml, request);
    }

    private List<String> configuredFeedUrls() {
        String configuredFeedUrls = System.getenv(FEED_URLS_ENV);
        if (configuredFeedUrls == null || configuredFeedUrls.isBlank()) {
            return List.of();
        }
        return Arrays.stream(configuredFeedUrls.split(","))
                .map(String::trim)
                .filter(feedUrl -> !feedUrl.isBlank())
                .toList();
    }
}
