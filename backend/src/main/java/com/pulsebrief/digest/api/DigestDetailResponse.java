package com.pulsebrief.digest.api;

import java.util.List;

public record DigestDetailResponse(
        Long id,
        String title,
        String sourceName,
        String digestType,
        String duration,
        String updatedAt,
        String summary,
        String audioText,
        List<String> points
) {
}
