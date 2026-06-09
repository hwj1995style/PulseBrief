package com.pulsebrief.admin.api;

public record AdminReportAssetResponse(
        Long id,
        String title,
        String originalUrl,
        String fileName,
        Long fileSizeBytes,
        String fileHash,
        String licensePolicy,
        String status
) {
}
