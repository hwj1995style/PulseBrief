package com.pulsebrief.admin.api;

public record AdminRawNewsItemResponse(
        Long id,
        String sourceCode,
        String providerItemId,
        String title,
        String summary,
        String sourceName,
        String originalUrl,
        String publishedAt,
        String fetchedAt,
        String language,
        String country,
        String status
) {
}
