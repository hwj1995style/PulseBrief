package com.pulsebrief.admin.api;

import java.util.List;

public record AdminCandidatePublishRequest(
        String title,
        String summary,
        String aiSummary,
        List<String> keyPoints,
        String impactAnalysis,
        String categoryCode,
        Boolean publishNow
) {
}
