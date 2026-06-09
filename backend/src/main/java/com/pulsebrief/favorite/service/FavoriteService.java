package com.pulsebrief.favorite.service;

import com.pulsebrief.article.api.ArticleCardResponse;
import com.pulsebrief.common.api.PageResponse;
import com.pulsebrief.favorite.api.FavoriteResponse;

public interface FavoriteService {
    FavoriteResponse favoriteArticle(Long userId, Long articleId);

    FavoriteResponse unfavoriteArticle(Long userId, Long articleId);

    PageResponse<ArticleCardResponse> listFavorites(Long userId, Integer page, Integer pageSize);
}
