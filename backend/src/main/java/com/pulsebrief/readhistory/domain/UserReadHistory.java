package com.pulsebrief.readhistory.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_read_history")
public class UserReadHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "article_id")
    private Long articleId;

    @Column(name = "read_time")
    private LocalDateTime readTime;

    protected UserReadHistory() {
    }

    public UserReadHistory(Long userId, Long articleId) {
        this.userId = userId;
        this.articleId = articleId;
        this.readTime = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getArticleId() {
        return articleId;
    }

    public LocalDateTime getReadTime() {
        return readTime;
    }
}
