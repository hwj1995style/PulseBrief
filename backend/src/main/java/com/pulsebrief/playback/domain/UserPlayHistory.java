package com.pulsebrief.playback.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_play_history")
public class UserPlayHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "article_id")
    private Long articleId;

    @Column(name = "digest_id")
    private Long digestId;

    @Column(name = "play_type")
    private String playType;

    @Column(name = "play_title")
    private String playTitle;

    @Column(name = "play_time")
    private LocalDateTime playTime;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    protected UserPlayHistory() {
    }

    public UserPlayHistory(
            Long userId,
            Long articleId,
            Long digestId,
            String playType,
            String playTitle,
            Integer durationSeconds
    ) {
        this.userId = userId;
        this.articleId = articleId;
        this.digestId = digestId;
        this.playType = playType;
        this.playTitle = playTitle;
        this.durationSeconds = durationSeconds;
        this.playTime = LocalDateTime.now();
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

    public Long getDigestId() {
        return digestId;
    }

    public String getPlayType() {
        return playType;
    }

    public String getPlayTitle() {
        return playTitle;
    }

    public LocalDateTime getPlayTime() {
        return playTime;
    }

    public Integer getDurationSeconds() {
        return durationSeconds;
    }
}
