package com.pulsebrief.admin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pulsebrief.admin.security")
public record AdminSecurityProperties(
        Boolean legacyTokenEnabled,
        String legacyToken,
        Integer sessionHours,
        Integer maxFailedAttempts,
        Integer lockMinutes,
        String bootstrapUsername,
        String bootstrapPassword,
        String bootstrapDisplayName,
        String bootstrapRole
) {
    public AdminSecurityProperties {
        legacyTokenEnabled = Boolean.TRUE.equals(legacyTokenEnabled);
        sessionHours = bounded(sessionHours, 12, 1, 72);
        maxFailedAttempts = bounded(maxFailedAttempts, 5, 3, 10);
        lockMinutes = bounded(lockMinutes, 15, 5, 1440);
        bootstrapRole = blankToDefault(bootstrapRole, "ADMIN").toUpperCase();
        bootstrapDisplayName = blankToDefault(bootstrapDisplayName, "PulseBrief Admin");
    }

    private static int bounded(Integer value, int defaultValue, int min, int max) {
        int resolved = value == null ? defaultValue : value;
        if (resolved < min || resolved > max) {
            throw new IllegalArgumentException("Admin security setting must be between " + min + " and " + max);
        }
        return resolved;
    }

    private static String blankToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }
}
