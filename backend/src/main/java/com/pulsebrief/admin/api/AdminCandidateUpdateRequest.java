package com.pulsebrief.admin.api;

public record AdminCandidateUpdateRequest(
        String title,
        String summary,
        String categoryCode,
        String sourceName
) {
}
