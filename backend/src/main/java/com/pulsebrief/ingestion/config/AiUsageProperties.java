package com.pulsebrief.ingestion.config;

import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pulsebrief.ai.usage")
public record AiUsageProperties(
        Boolean enabled,
        Integer dailyRequestLimit,
        Integer dailyTokenLimit,
        Integer warningPercent,
        BigDecimal deepSeekInputCostPerMillionUsd,
        BigDecimal deepSeekOutputCostPerMillionUsd
) {
    public AiUsageProperties {
        enabled = enabled == null || enabled;
        dailyRequestLimit = bounded(dailyRequestLimit, 200, 1, 100000);
        dailyTokenLimit = bounded(dailyTokenLimit, 200000, 1000, 100000000);
        warningPercent = bounded(warningPercent, 80, 1, 100);
        deepSeekInputCostPerMillionUsd = nonNegative(deepSeekInputCostPerMillionUsd);
        deepSeekOutputCostPerMillionUsd = nonNegative(deepSeekOutputCostPerMillionUsd);
    }

    private static int bounded(Integer value, int defaultValue, int min, int max) {
        int resolved = value == null ? defaultValue : value;
        if (resolved < min || resolved > max) {
            throw new IllegalArgumentException("AI usage setting must be between " + min + " and " + max);
        }
        return resolved;
    }

    private static BigDecimal nonNegative(BigDecimal value) {
        BigDecimal resolved = value == null ? BigDecimal.ZERO : value;
        if (resolved.signum() < 0) {
            throw new IllegalArgumentException("AI cost setting must not be negative");
        }
        return resolved;
    }
}
