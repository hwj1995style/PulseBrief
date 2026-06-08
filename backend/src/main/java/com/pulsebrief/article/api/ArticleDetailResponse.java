package com.pulsebrief.article.api;

import java.util.List;

public record ArticleDetailResponse(
        Long id,
        String title,
        String sourceName,
        String publishTime,
        String categoryCode,
        String categoryName,
        String aiSummary,
        List<String> keyPoints,
        String impactAnalysis,
        String originalUrl,
        Boolean favorited,
        List<ArticleCardResponse> relatedArticles
) {
}
