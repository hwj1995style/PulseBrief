package com.pulsebrief.article.repository;

import com.pulsebrief.article.domain.NewsArticle;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArticleRepository extends JpaRepository<NewsArticle, Long> {
    List<NewsArticle> findByArticleStatusOrderByTopDescHotScoreDescPublishTimeDesc(String status, Pageable pageable);

    List<NewsArticle> findByArticleStatusAndCategoryCodeOrderByTopDescHotScoreDescPublishTimeDesc(
            String status,
            String categoryCode,
            Pageable pageable
    );

    Optional<NewsArticle> findByIdAndArticleStatus(Long id, String status);

    Optional<NewsArticle> findFirstByArticleStatusAndCategoryCodeOrderByHotScoreDescPublishTimeDesc(
            String status,
            String categoryCode
    );
}
