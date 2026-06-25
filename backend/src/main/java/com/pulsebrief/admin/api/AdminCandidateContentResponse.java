package com.pulsebrief.admin.api;

import java.time.LocalDateTime;

public record AdminCandidateContentResponse(
        Long candidateId,
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
