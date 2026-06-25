package com.pulsebrief.ingestion.service;

public record AiSummaryInput(
        String sourceType,
        Long refId,
        String text,
        String textHash,
        String preview,
        String licenseNote
) {
}
