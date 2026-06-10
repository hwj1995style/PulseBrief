package com.pulsebrief.digest.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "daily_digest")
public class DailyDigest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "digest_date")
    private LocalDate digestDate;

    @Column(name = "digest_type")
    private String digestType;

    @Column(name = "category_code")
    private String categoryCode;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "audio_text", columnDefinition = "TEXT")
    private String audioText;

    @Column(name = "digest_status")
    private String digestStatus;

    @Column(name = "publish_time")
    private LocalDateTime publishTime;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    protected DailyDigest() {
    }

    public DailyDigest(
            LocalDate digestDate,
            String digestType,
            String categoryCode,
            String title,
            String summary,
            String content,
            String audioText
    ) {
        this.digestDate = digestDate;
        this.digestType = digestType;
        this.categoryCode = categoryCode;
        this.title = title;
        this.summary = summary;
        this.content = content;
        this.audioText = audioText;
        this.digestStatus = "DRAFT";
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    public Long getId() {
        return id;
    }

    public LocalDate getDigestDate() {
        return digestDate;
    }

    public String getDigestType() {
        return digestType;
    }

    public String getCategoryCode() {
        return categoryCode;
    }

    public String getTitle() {
        return title;
    }

    public String getSummary() {
        return summary;
    }

    public String getContent() {
        return content;
    }

    public String getAudioText() {
        return audioText;
    }

    public LocalDateTime getPublishTime() {
        return publishTime;
    }

    public String getDigestStatus() {
        return digestStatus;
    }

    public void updateDraft(
            LocalDate digestDate,
            String digestType,
            String title,
            String summary,
            String content,
            String audioText,
            String categoryCode
    ) {
        this.digestDate = digestDate;
        this.digestType = digestType;
        this.title = title;
        this.summary = summary;
        this.content = content;
        this.audioText = audioText;
        this.categoryCode = categoryCode;
        this.updatedAt = LocalDateTime.now();
    }

    public void publish(LocalDateTime publishTime) {
        this.digestStatus = "PUBLISHED";
        this.publishTime = publishTime;
        this.updatedAt = publishTime;
    }

    public void offline() {
        this.digestStatus = "OFFLINE";
        this.updatedAt = LocalDateTime.now();
    }
}
