package com.pulsebrief.ingestion.service;

public record IngestionResult(
        Long jobId,
        Integer fetchedCount,
        Integer newCount,
        Integer duplicateCount,
        Integer candidateCount
) {
}
