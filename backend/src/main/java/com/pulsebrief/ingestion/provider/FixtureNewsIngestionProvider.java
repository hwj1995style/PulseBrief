package com.pulsebrief.ingestion.provider;

import com.pulsebrief.ingestion.service.IngestionRequest;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class FixtureNewsIngestionProvider implements NewsIngestionProvider {
    private static final List<RawNewsPayload> FIXTURE_ITEMS = List.of(
            new RawNewsPayload(
                    "fixture-ai-infra",
                    "AI infrastructure investment remains resilient",
                    "Public market commentary highlights continued demand for compute, power, and data center capacity.",
                    "Example Markets",
                    "https://example.com/ai-infra",
                    null,
                    OffsetDateTime.parse("2026-06-09T09:00:00+08:00"),
                    "en",
                    "US",
                    "{\"id\":\"fixture-ai-infra\",\"topic\":\"ai\"}"
            ),
            new RawNewsPayload(
                    "fixture-central-bank",
                    "Central bank officials keep cautious tone",
                    "Investors reassess rate expectations after public remarks from policy officials.",
                    "Example Policy",
                    "https://example.com/central-bank",
                    null,
                    OffsetDateTime.parse("2026-06-09T10:00:00+08:00"),
                    "en",
                    "US",
                    "{\"id\":\"fixture-central-bank\",\"topic\":\"macro\"}"
            ),
            new RawNewsPayload(
                    "fixture-semiconductor",
                    "Semiconductor supply chain attention shifts to advanced packaging",
                    "Market attention remains on HBM, advanced packaging, and high-speed interconnect demand.",
                    "Example Industry",
                    "https://example.com/semiconductor-packaging",
                    null,
                    OffsetDateTime.parse("2026-06-09T11:00:00+08:00"),
                    "en",
                    "US",
                    "{\"id\":\"fixture-semiconductor\",\"topic\":\"technology\"}"
            )
    );

    @Override
    public String providerType() {
        return "FIXTURE";
    }

    @Override
    public List<RawNewsPayload> fetch(IngestionRequest request) {
        int pageSize = request.pageSizeOrDefault();
        return FIXTURE_ITEMS.stream()
                .filter(item -> matchesKeywords(item, request.keywords()))
                .limit(pageSize)
                .toList();
    }

    private boolean matchesKeywords(RawNewsPayload item, List<String> keywords) {
        if (keywords == null || keywords.isEmpty()) {
            return true;
        }
        String searchable = (item.title() + " " + item.summary()).toLowerCase(Locale.ROOT);
        return keywords.stream()
                .map(keyword -> keyword.toLowerCase(Locale.ROOT).trim())
                .filter(keyword -> !keyword.isBlank())
                .anyMatch(searchable::contains);
    }
}
