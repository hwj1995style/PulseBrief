package com.pulsebrief.favorite.service;

import com.pulsebrief.favorite.api.FavoriteResponse;
import com.pulsebrief.favorite.domain.UserFavorite;
import com.pulsebrief.favorite.repository.UserFavoriteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FavoriteApplicationService implements FavoriteService {
    private final UserFavoriteRepository favoriteRepository;

    public FavoriteApplicationService(UserFavoriteRepository favoriteRepository) {
        this.favoriteRepository = favoriteRepository;
    }

    @Override
    @Transactional
    public FavoriteResponse favoriteArticle(Long userId, Long articleId) {
        favoriteRepository.findByUserIdAndArticleId(userId, articleId)
                .orElseGet(() -> favoriteRepository.save(new UserFavorite(userId, articleId)));
        return new FavoriteResponse(articleId, true);
    }

    @Override
    @Transactional
    public FavoriteResponse unfavoriteArticle(Long userId, Long articleId) {
        favoriteRepository.deleteByUserIdAndArticleId(userId, articleId);
        return new FavoriteResponse(articleId, false);
    }
}
