package com.pulsebrief.ingestion.domain;

import com.pulsebrief.ingestion.provider.RawNewsPayload;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "raw_news_item")
public class RawNewsItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_code")
    private String sourceCode;

    @Column(name = "provider_item_id")
    private String providerItemId;

    private String title;

    private String summary;

    @Column(name = "source_name")
    private String sourceName;

    @Column(name = "original_url")
    private String originalUrl;

    @Column(name = "original_url_hash")
    private String originalUrlHash;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "fetched_at")
    private LocalDateTime fetchedAt;

    private String language;

    private String country;

    @Column(name = "raw_payload")
    private String rawPayload;

    @Column(name = "content_hash")
    private String contentHash;

    @Column(name = "item_status")
    private String itemStatus;

    @Column(name = "duplicate_of_id")
    private Long duplicateOfId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    protected RawNewsItem() {
    }

    public RawNewsItem(String sourceCode, RawNewsPayload payload, String originalUrlHash, String contentHash) {
        LocalDateTime now = LocalDateTime.now();
        this.sourceCode = sourceCode;
        this.providerItemId = payload.providerItemId();
        this.title = payload.title().trim();
        this.summary = payload.summary();
        this.sourceName = payload.sourceName();
        this.originalUrl = payload.originalUrl().trim();
        this.originalUrlHash = originalUrlHash;
        this.imageUrl = payload.imageUrl();
        this.publishedAt = payload.publishedAt() == null ? null : payload.publishedAt().toLocalDateTime();
        this.fetchedAt = now;
        this.language = payload.language();
        this.country = payload.country();
        this.rawPayload = payload.rawPayload();
        this.contentHash = contentHash;
        this.itemStatus = "NEW";
        this.createdAt = now;
        this.updatedAt = now;
    }

    public Long getId() {
        return id;
    }

    public String getOriginalUrl() {
        return originalUrl;
    }

    public String getTitle() {
        return title;
    }

    public String getSummary() {
        return summary;
    }

    public String getSourceCode() {
        return sourceCode;
    }

    public String getSourceName() {
        return sourceName;
    }

    public String getProviderItemId() {
        return providerItemId;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public LocalDateTime getFetchedAt() {
        return fetchedAt;
    }

    public String getLanguage() {
        return language;
    }

    public String getCountry() {
        return country;
    }

    public String getItemStatus() {
        return itemStatus;
    }

    public void markAsCandidate() {
        this.itemStatus = "CANDIDATE";
        this.updatedAt = LocalDateTime.now();
    }

    public void markAsRejected() {
        this.itemStatus = "REJECTED";
        this.updatedAt = LocalDateTime.now();
    }

    public void markAsPublished() {
        this.itemStatus = "PUBLISHED";
        this.updatedAt = LocalDateTime.now();
    }
}
