package com.pulsebrief.admin.api;

import java.util.List;

public record AdminCandidateResponse(
        Long id,
        Long rawNewsItemId,
        String title,
        String summary,
        String categoryCode,
        String sourceName,
        String originalUrl,
        String publishedAt,
        String status,
        String createdAt,
        Long publishedArticleId,
        String reviewNote,
        List<String> tagNames
) {
}
