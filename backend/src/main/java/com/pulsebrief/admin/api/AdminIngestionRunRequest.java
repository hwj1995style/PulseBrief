package com.pulsebrief.admin.api;

public record AdminIngestionRunRequest(
        Integer pageSize,
        Boolean generateCandidates
) {
    public int pageSizeOrDefault() {
        if (pageSize == null || pageSize <= 0) {
            return 5;
        }
        return Math.min(pageSize, 20);
    }

    public boolean shouldGenerateCandidates() {
        return generateCandidates == null || generateCandidates;
    }
}
