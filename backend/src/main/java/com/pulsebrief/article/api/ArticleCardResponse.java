package com.pulsebrief.article.api;

public record ArticleCardResponse(
        Long id,
        String title,
        String sourceName,
        String publishTime,
        String categoryCode,
        String categoryName,
        String summary,
        String imageUrl,
        String audioDuration,
        Boolean hot,
        Boolean breaking,
        Boolean favorited
) {
}
