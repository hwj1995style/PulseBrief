package com.pulsebrief.admin.api;

public record AdminReportAssetResponse(
        Long id,
        String title,
        String originalUrl,
        String fileName,
        Long fileSizeBytes,
        String fileHash,
        String licensePolicy,
        String status,
        String licenseNote,
        String cacheStatus,
        String cacheErrorMessage,
        String mimeType,
        String cachedAt,
        String reviewNote,
        String reviewedAt,
        String reviewedBy
) {
}
