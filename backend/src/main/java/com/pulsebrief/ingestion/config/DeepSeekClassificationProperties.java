package com.pulsebrief.ingestion.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pulsebrief.ai.deepseek.classification")
public record DeepSeekClassificationProperties(
        Boolean enabled,
        Double minConfidence,
        Integer maxInputCharacters,
        Integer maxOutputTokens
) {
    public DeepSeekClassificationProperties {
        enabled = Boolean.TRUE.equals(enabled);
        minConfidence = bounded(minConfidence, 0.65, 0.0, 1.0);
        maxInputCharacters = bounded(maxInputCharacters, 4000, 200, 12000);
        maxOutputTokens = bounded(maxOutputTokens, 300, 100, 1000);
    }

    private static int bounded(Integer value, int defaultValue, int min, int max) {
        int resolved = value == null ? defaultValue : value;
        if (resolved < min || resolved > max) {
            throw new IllegalArgumentException("DeepSeek classification setting must be between " + min + " and " + max);
        }
        return resolved;
    }

    private static double bounded(Double value, double defaultValue, double min, double max) {
        double resolved = value == null ? defaultValue : value;
        if (resolved < min || resolved > max) {
            throw new IllegalArgumentException("DeepSeek classification confidence must be between " + min + " and " + max);
        }
        return resolved;
    }
}
