package com.pulsebrief.digest.api;

public record DigestSummaryResponse(
        Long id,
        String title,
        String subtitle,
        String duration,
        String updateTime
) {
}
