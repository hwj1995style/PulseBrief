package com.pulsebrief.admin.api;

public record AdminDigestArticleRequest(
        Long articleId,
        Integer sortNo,
        String highlightText
) {
}
