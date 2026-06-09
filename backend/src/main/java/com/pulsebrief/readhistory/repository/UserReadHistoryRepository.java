package com.pulsebrief.readhistory.repository;

import com.pulsebrief.article.domain.NewsArticle;
import com.pulsebrief.readhistory.domain.UserReadHistory;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface UserReadHistoryRepository extends JpaRepository<UserReadHistory, Long> {
    Integer countByUserId(Long userId);

    @Query("""
            select article
            from NewsArticle article
            join UserReadHistory history on history.articleId = article.id
            where history.userId = :userId
              and article.articleStatus = 'PUBLISHED'
            order by history.readTime desc
            """)
    List<NewsArticle> findReadArticles(@Param("userId") Long userId, Pageable pageable);

    @Transactional
    void deleteByUserId(Long userId);
}
