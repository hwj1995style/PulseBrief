package com.pulsebrief.favorite.service;

import com.pulsebrief.article.api.ArticleCardResponse;
import com.pulsebrief.favorite.api.FavoriteResponse;
import java.util.List;

public interface FavoriteService {
    FavoriteResponse favoriteArticle(Long userId, Long articleId);

    FavoriteResponse unfavoriteArticle(Long userId, Long articleId);

    List<ArticleCardResponse> listFavorites(Long userId, Integer page, Integer pageSize);
}
