package com.pulsebrief.favorite.service;

import com.pulsebrief.favorite.api.FavoriteResponse;

public interface FavoriteService {
    FavoriteResponse favoriteArticle(Long userId, Long articleId);

    FavoriteResponse unfavoriteArticle(Long userId, Long articleId);
}
