package com.pulsebrief.admin.api;

import java.math.BigDecimal;

public record AdminAiUsageResponse(
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
