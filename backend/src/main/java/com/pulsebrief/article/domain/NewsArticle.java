package com.pulsebrief.article.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "news_article")
public class NewsArticle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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

    @Column(name = "article_hash")
    private String articleHash;

    @Column(name = "tag_names")
    private String tagNames;

    protected NewsArticle() {
    }

    public NewsArticle(
            String title,
            String summary,
            String aiSummary,
            String keyPoints,
            String impactAnalysis,
            String sourceName,
            String originalUrl,
            String categoryCode,
            LocalDateTime publishTime,
            String articleHash
    ) {
        this(
                title,
                summary,
                aiSummary,
                keyPoints,
                impactAnalysis,
                sourceName,
                originalUrl,
                categoryCode,
                publishTime,
                articleHash,
                null
        );
    }

    public NewsArticle(
            String title,
            String summary,
            String aiSummary,
            String keyPoints,
            String impactAnalysis,
            String sourceName,
            String originalUrl,
            String categoryCode,
            LocalDateTime publishTime,
            String articleHash,
            String tagNames
    ) {
        this.title = title;
        this.summary = summary;
        this.aiSummary = aiSummary;
        this.keyPoints = keyPoints;
        this.impactAnalysis = impactAnalysis;
        this.sourceName = sourceName;
        this.originalUrl = originalUrl;
        this.categoryCode = categoryCode;
        this.publishTime = publishTime;
        this.hotScore = BigDecimal.ZERO;
        this.articleStatus = "PUBLISHED";
        this.top = 0;
        this.articleHash = articleHash;
        this.tagNames = tagNames;
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

    public String getArticleStatus() {
        return articleStatus;
    }

    public String getTagNames() {
        return tagNames;
    }
}
