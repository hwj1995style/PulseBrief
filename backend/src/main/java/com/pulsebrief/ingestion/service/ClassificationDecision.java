package com.pulsebrief.ingestion.service;

public record ClassificationDecision(
        String suggestedCategoryCode,
        double confidence,
        String matchedRule
) {
}
