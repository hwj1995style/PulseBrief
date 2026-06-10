package com.pulsebrief.admin.api;

import java.util.List;

public record AdminDigestResponse(
        Long id,
        String digestDate,
        String digestType,
        String categoryCode,
        String title,
        String summary,
        String content,
        String audioText,
        String status,
        String publishTime,
        Long articleCount,
        List<AdminDigestArticleResponse> articles,
        List<String> availableActions
) {
}
