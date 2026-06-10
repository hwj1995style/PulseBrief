package com.pulsebrief.admin.api;

import java.util.List;

public record AdminDigestCreateRequest(
        String digestDate,
        String digestType,
        String categoryCode,
        String title,
        String summary,
        String content,
        String audioText,
        List<AdminDigestArticleRequest> articles
) {
}
