package com.pulsebrief.favorite.service;

import com.pulsebrief.article.api.ArticleCardResponse;
import com.pulsebrief.article.service.ArticleCardMapper;
import com.pulsebrief.common.api.PageResponse;
import com.pulsebrief.favorite.api.FavoriteResponse;
import com.pulsebrief.favorite.domain.UserFavorite;
import com.pulsebrief.favorite.repository.UserFavoriteRepository;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FavoriteApplicationService implements FavoriteService {
    private final UserFavoriteRepository favoriteRepository;
    private final ArticleCardMapper articleCardMapper;

    public FavoriteApplicationService(
            UserFavoriteRepository favoriteRepository,
            ArticleCardMapper articleCardMapper
    ) {
        this.favoriteRepository = favoriteRepository;
        this.articleCardMapper = articleCardMapper;
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

    @Override
    public PageResponse<ArticleCardResponse> listFavorites(Long userId, Integer page, Integer pageSize) {
        PageRequest pageable = PageRequest.of(safePage(page), safePageSize(pageSize));
        List<ArticleCardResponse> items = favoriteRepository.findFavoriteArticles(userId, pageable)
                .stream()
                .map(articleCardMapper::toFavoriteCard)
                .toList();
        return PageResponse.of(items, page, pageSize, favoriteRepository.countByUserId(userId).longValue());
    }

    private int safePage(Integer page) {
        return Math.max(page == null ? 1 : page, 1) - 1;
    }

    private int safePageSize(Integer pageSize) {
        return Math.min(Math.max(pageSize == null ? 20 : pageSize, 1), 50);
    }
}
