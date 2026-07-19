package com.pulsebrief.admin.api;

import java.util.List;

public record AdminCandidateUpdateRequest(
        String title,
        String summary,
        String categoryCode,
        String categoryOverrideReason,
        String sourceName,
        List<String> tagNames
) {
}
