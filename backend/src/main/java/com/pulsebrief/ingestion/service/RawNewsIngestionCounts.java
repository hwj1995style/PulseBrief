package com.pulsebrief.ingestion.service;

public record RawNewsIngestionCounts(
        int fetchedCount,
        int newCount,
        int duplicateCount
) {
}
