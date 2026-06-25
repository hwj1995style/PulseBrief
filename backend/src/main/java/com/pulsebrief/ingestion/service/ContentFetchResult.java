package com.pulsebrief.ingestion.service;

import java.time.LocalDateTime;

public record ContentFetchResult(
        Long rawNewsItemId,
        String captureMode,
        String fetchStatus,
        String preview,
        String licensePolicy,
        String licenseNote,
        LocalDateTime fetchedAt,
        String errorMessage
) {
}
