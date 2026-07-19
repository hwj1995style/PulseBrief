package com.pulsebrief.ingestion.service;

import java.math.BigDecimal;

public record AiUsageSnapshot(
        long requestCount,
        long successCount,
        long failedCount,
        long blockedCount,
        long promptTokens,
        long completionTokens,
        BigDecimal estimatedCostUsd,
        int dailyRequestLimit,
        int dailyTokenLimit,
        int warningPercent,
        String alertLevel
) {
}
