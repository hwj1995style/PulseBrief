package com.pulsebrief.ingestion.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import com.pulsebrief.ingestion.service.ClassificationDecision;

@Entity
@Table(name = "candidate_article")
public class CandidateArticle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "raw_news_item_id")
    private RawNewsItem rawNewsItem;

    private String title;

    private String summary;

    @Column(name = "category_code")
    private String categoryCode;

    @Column(name = "suggested_category_code")
    private String suggestedCategoryCode;

    @Column(name = "classification_confidence")
    private BigDecimal classificationConfidence;

    @Column(name = "classification_rule")
    private String classificationRule;

    @Column(name = "category_override_reason")
    private String categoryOverrideReason;

    @Column(name = "source_name")
    private String sourceName;

    @Column(name = "original_url")
    private String originalUrl;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "candidate_status")
    private String status;

    @Column(name = "review_note")
    private String reviewNote;

    @Column(name = "tag_names")
    private String tagNames;

    @Column(name = "published_article_id")
    private Long publishedArticleId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    protected CandidateArticle() {
    }

    public CandidateArticle(RawNewsItem rawNewsItem, ClassificationDecision classification) {
        LocalDateTime now = LocalDateTime.now();
        this.rawNewsItem = rawNewsItem;
        this.title = rawNewsItem.getTitle();
        this.summary = rawNewsItem.getSummary();
        this.categoryCode = classification.suggestedCategoryCode();
        this.suggestedCategoryCode = classification.suggestedCategoryCode();
        this.classificationConfidence = BigDecimal.valueOf(classification.confidence());
        this.classificationRule = classification.matchedRule();
        this.sourceName = rawNewsItem.getSourceName();
        this.originalUrl = rawNewsItem.getOriginalUrl();
        this.publishedAt = rawNewsItem.getPublishedAt();
        this.status = "PENDING_REVIEW";
        this.createdAt = now;
        this.updatedAt = now;
    }

    public Long getId() {
        return id;
    }

    public RawNewsItem getRawNewsItem() {
        return rawNewsItem;
    }

    public String getTitle() {
        return title;
    }

    public String getSummary() {
        return summary;
    }

    public String getCategoryCode() {
        return categoryCode;
    }

    public String getSuggestedCategoryCode() {
        return suggestedCategoryCode;
    }

    public Double getClassificationConfidence() {
        return classificationConfidence == null ? null : classificationConfidence.doubleValue();
    }

    public String getClassificationRule() {
        return classificationRule;
    }

    public String getCategoryOverrideReason() {
        return categoryOverrideReason;
    }

    public String getSourceName() {
        return sourceName;
    }

    public String getOriginalUrl() {
        return originalUrl;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public String getStatus() {
        return status;
    }

    public String getReviewNote() {
        return reviewNote;
    }

    public String getTagNames() {
        return tagNames;
    }

    public Long getPublishedArticleId() {
        return publishedArticleId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void updateDraft(
            String title,
            String summary,
            String categoryCode,
            String sourceName,
            String tagNames,
            String categoryOverrideReason
    ) {
        this.title = title.trim();
        this.summary = summary;
        this.categoryCode = categoryCode;
        this.categoryOverrideReason = categoryCode.equals(this.suggestedCategoryCode)
                ? null
                : categoryOverrideReason.trim();
        this.sourceName = sourceName;
        this.tagNames = tagNames;
        this.updatedAt = LocalDateTime.now();
    }

    public void reject(String reviewNote) {
        this.status = "REJECTED";
        this.reviewNote = reviewNote;
        this.updatedAt = LocalDateTime.now();
        this.rawNewsItem.markAsRejected();
    }

    public void confirmCategory(String categoryCode, String overrideReason) {
        this.categoryCode = categoryCode;
        this.categoryOverrideReason = categoryCode.equals(this.suggestedCategoryCode) ? null : overrideReason.trim();
        this.updatedAt = LocalDateTime.now();
    }

    public void publish(Long publishedArticleId) {
        this.status = "PUBLISHED";
        this.publishedArticleId = publishedArticleId;
        this.updatedAt = LocalDateTime.now();
        this.rawNewsItem.markAsPublished();
    }
}
