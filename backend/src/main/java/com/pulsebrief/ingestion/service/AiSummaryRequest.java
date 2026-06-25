package com.pulsebrief.ingestion.service;

import java.time.LocalDateTime;

public record AiSummaryRequest(
        String title,
        String sourceName,
        LocalDateTime publishedAt,
        String inputSourceType,
        String inputText,
        String inputPreview,
        String promptVersion
) {
}
