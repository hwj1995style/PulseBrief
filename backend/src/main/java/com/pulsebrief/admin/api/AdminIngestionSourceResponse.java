package com.pulsebrief.admin.api;

public record AdminIngestionSourceResponse(
        Long id,
        String code,
        String name,
        String providerType,
        String defaultCategoryCode,
        Boolean enabled,
        Boolean scheduleEnabled,
        Integer scheduleIntervalMinutes,
        java.time.LocalDateTime nextRunAt,
        String contentAccessPolicy,
        Integer maxAgeHours,
        Boolean allowPdfDownload,
        Boolean allowFullText
) {
}
