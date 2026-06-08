package com.pulsebrief.digest.api;

import java.util.List;

public record TodayDigestResponse(
        String date,
        DigestSummaryResponse headline,
        List<DigestSummaryResponse> digests,
        List<String> highlights
) {
}
