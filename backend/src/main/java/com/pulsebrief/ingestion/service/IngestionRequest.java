package com.pulsebrief.ingestion.service;

import java.util.List;

public record IngestionRequest(
        String sourceCode,
        List<String> keywords,
        String language,
        String country,
        String market,
        Integer pageSize
) {
    public IngestionRequest {
        keywords = keywords == null ? List.of() : List.copyOf(keywords);
    }

    public int pageSizeOrDefault() {
        if (pageSize == null || pageSize <= 0) {
            return 20;
        }
        return Math.min(pageSize, 100);
    }
}
