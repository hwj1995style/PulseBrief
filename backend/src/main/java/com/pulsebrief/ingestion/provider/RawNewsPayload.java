package com.pulsebrief.ingestion.provider;

import java.time.OffsetDateTime;

public record RawNewsPayload(
        String providerItemId,
        String title,
        String summary,
        String sourceName,
        String originalUrl,
        String imageUrl,
        OffsetDateTime publishedAt,
        String language,
        String country,
        String rawPayload
) {
}
