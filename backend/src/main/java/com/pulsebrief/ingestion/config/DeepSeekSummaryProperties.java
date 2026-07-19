package com.pulsebrief.ingestion.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pulsebrief.ai.deepseek")
public record DeepSeekSummaryProperties(
        Boolean enabled,
        String apiKey,
        String baseUrl,
        String model,
        Integer timeoutSeconds,
        Integer maxInputCharacters,
        Integer maxOutputTokens
) {
    public DeepSeekSummaryProperties {
        enabled = Boolean.TRUE.equals(enabled);
        baseUrl = blankToDefault(baseUrl, "https://api.deepseek.com/chat/completions");
        model = blankToDefault(model, "deepseek-v4-flash");
        timeoutSeconds = bounded(timeoutSeconds, 30, 5, 120);
        maxInputCharacters = bounded(maxInputCharacters, 12000, 500, 50000);
        maxOutputTokens = bounded(maxOutputTokens, 1200, 300, 4000);
    }

    private static String blankToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    private static int bounded(Integer value, int defaultValue, int min, int max) {
        int resolved = value == null ? defaultValue : value;
        if (resolved < min || resolved > max) {
            throw new IllegalArgumentException("DeepSeek summary setting must be between " + min + " and " + max);
        }
        return resolved;
    }
}
