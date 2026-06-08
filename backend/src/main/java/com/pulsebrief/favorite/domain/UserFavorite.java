package com.pulsebrief.favorite.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_favorite")
public class UserFavorite {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "article_id")
    private Long articleId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    protected UserFavorite() {
    }

    public UserFavorite(Long userId, Long articleId) {
        this.userId = userId;
        this.articleId = articleId;
        this.createdAt = LocalDateTime.now();
    }
}
