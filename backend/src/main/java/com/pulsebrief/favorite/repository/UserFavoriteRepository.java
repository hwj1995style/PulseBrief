package com.pulsebrief.favorite.repository;

import com.pulsebrief.article.domain.NewsArticle;
import com.pulsebrief.favorite.domain.UserFavorite;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface UserFavoriteRepository extends JpaRepository<UserFavorite, Long> {
    Optional<UserFavorite> findByUserIdAndArticleId(Long userId, Long articleId);

    Integer countByUserId(Long userId);

    @Query("""
            select article
            from NewsArticle article
            join UserFavorite favorite on favorite.articleId = article.id
            where favorite.userId = :userId
              and article.articleStatus = 'PUBLISHED'
            order by favorite.createdAt desc
            """)
    List<NewsArticle> findFavoriteArticles(@Param("userId") Long userId, Pageable pageable);

    @Transactional
    void deleteByUserIdAndArticleId(Long userId, Long articleId);
}
