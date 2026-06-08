package com.pulsebrief.article.api;

import java.util.List;

public record HomeArticlesResponse(
        DigestHeroResponse todayDigest,
        ArticleCardResponse investmentPick,
        List<ArticleCardResponse> articles
) {
}
