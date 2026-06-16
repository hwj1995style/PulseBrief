package com.pulsebrief.admin.api;

public record AdminIngestionRunResponse(
        Long jobId,
        String sourceCode,
        String providerType,
        String status,
        Integer fetchedCount,
        Integer newCount,
        Integer duplicateCount,
        Integer candidateCount,
        String errorMessage
) {
}
