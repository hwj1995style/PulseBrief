package com.pulsebrief.admin.api;

public record AdminDigestArticleResponse(
        Long articleId,
        Integer sortNo,
        String highlightText,
        String title,
        String sourceName
) {
}
