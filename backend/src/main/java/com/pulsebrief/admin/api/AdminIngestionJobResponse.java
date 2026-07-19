package com.pulsebrief.admin.api;

import java.time.LocalDateTime;

public record AdminIngestionJobResponse(
        Long id,
        String sourceCode,
        String triggerType,
        String status,
        Integer attemptCount,
        Integer maxAttempts,
        LocalDateTime nextRetryAt,
        Boolean cancelRequested,
        LocalDateTime startedAt,
        LocalDateTime finishedAt,
        Integer fetchedCount,
        Integer newCount,
        Integer duplicateCount,
        Integer candidateCount,
        String errorMessage
) {
}
