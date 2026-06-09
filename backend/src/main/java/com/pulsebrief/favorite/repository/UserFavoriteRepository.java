package com.pulsebrief.favorite.repository;

import com.pulsebrief.favorite.domain.UserFavorite;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

public interface UserFavoriteRepository extends JpaRepository<UserFavorite, Long> {
    Optional<UserFavorite> findByUserIdAndArticleId(Long userId, Long articleId);

    Integer countByUserId(Long userId);

    @Transactional
    void deleteByUserIdAndArticleId(Long userId, Long articleId);
}
