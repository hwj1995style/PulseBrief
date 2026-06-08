package com.pulsebrief.article.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "news_article")
public class NewsArticle {
    @Id
    private Long id;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(name = "ai_summary", columnDefinition = "TEXT")
    private String aiSummary;

    @Column(name = "key_points", columnDefinition = "TEXT")
    private String keyPoints;

    @Column(name = "impact_analysis", columnDefinition = "TEXT")
    private String impactAnalysis;

    @Column(name = "source_name")
    private String sourceName;

    @Column(name = "original_url")
    private String originalUrl;

    @Column(name = "category_code")
    private String categoryCode;

    @Column(name = "publish_time")
    private LocalDateTime publishTime;

    @Column(name = "hot_score")
    private BigDecimal hotScore;

    @Column(name = "article_status")
    private String articleStatus;

    @Column(name = "is_top")
    private Byte top;

    protected NewsArticle() {
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getSummary() {
        return summary;
    }

    public String getAiSummary() {
        return aiSummary;
    }

    public String getKeyPoints() {
        return keyPoints;
    }

    public String getImpactAnalysis() {
        return impactAnalysis;
    }

    public String getSourceName() {
        return sourceName;
    }

    public String getOriginalUrl() {
        return originalUrl;
    }

    public String getCategoryCode() {
        return categoryCode;
    }

    public LocalDateTime getPublishTime() {
        return publishTime;
    }

    public BigDecimal getHotScore() {
        return hotScore;
    }

    public Byte getTop() {
        return top;
    }
}
