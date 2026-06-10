package com.pulsebrief.admin.api;

public record AdminIngestionMetricsResponse(
        Long fetchedCount,
        Long candidateCount,
        Long publishedCount,
        Long failedCount
) {
}
