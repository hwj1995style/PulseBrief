package com.pulsebrief.admin.api;

import java.time.LocalDateTime;

public record AdminIngestionAnomalyResponse(
        Long id,
        Long rawNewsItemId,
        String title,
        String sourceCode,
        String sourceName,
        String originalUrl,
        LocalDateTime publishedAt,
        LocalDateTime fetchedAt,
        String issueType,
        String severity,
        String description
) {
}
