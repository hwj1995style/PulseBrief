package com.pulsebrief.ingestion.service;

import java.util.List;

public record AiSummaryProviderResult(
        String summary,
        List<String> keyPoints,
        String impactAnalysis,
        String modelName,
        Integer tokenPromptCount,
        Integer tokenCompletionCount
) {
}
