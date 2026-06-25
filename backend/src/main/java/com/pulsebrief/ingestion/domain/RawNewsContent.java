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

@Entity
@Table(name = "raw_news_content")
public class RawNewsContent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "raw_news_item_id")
    private RawNewsItem rawNewsItem;

    @Column(name = "source_code")
    private String sourceCode;

    @Column(name = "capture_mode")
    private String captureMode;

    @Column(name = "content_text")
    private String contentText;

    @Column(name = "content_text_hash")
    private String contentTextHash;

    @Column(name = "license_policy")
    private String licensePolicy;

    @Column(name = "license_note")
    private String licenseNote;

    @Column(name = "fetch_status")
    private String fetchStatus;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "fetched_at")
    private LocalDateTime fetchedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    protected RawNewsContent() {
    }

    private RawNewsContent(
            RawNewsItem rawNewsItem,
            String captureMode,
            String licensePolicy,
            String licenseNote,
            String fetchStatus,
            String contentText,
            String contentTextHash,
            String errorMessage
    ) {
        LocalDateTime now = LocalDateTime.now();
        this.rawNewsItem = rawNewsItem;
        this.sourceCode = rawNewsItem.getSourceCode();
        this.captureMode = captureMode;
        this.licensePolicy = licensePolicy;
        this.licenseNote = licenseNote;
        this.fetchStatus = fetchStatus;
        this.contentText = contentText;
        this.contentTextHash = contentTextHash;
        this.errorMessage = errorMessage;
        this.fetchedAt = now;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public static RawNewsContent success(
            RawNewsItem rawNewsItem,
            String captureMode,
            String licensePolicy,
            String licenseNote,
            String contentText,
            String contentTextHash
    ) {
        return new RawNewsContent(
                rawNewsItem,
                captureMode,
                licensePolicy,
                licenseNote,
                "SUCCESS",
                contentText,
                contentTextHash,
                null
        );
    }

    public static RawNewsContent skipped(
            RawNewsItem rawNewsItem,
            String captureMode,
            String licensePolicy,
            String licenseNote,
            String errorMessage
    ) {
        return new RawNewsContent(
                rawNewsItem,
                captureMode,
                licensePolicy,
                licenseNote,
                "SKIPPED",
                null,
                null,
                errorMessage
        );
    }

    public static RawNewsContent failed(
            RawNewsItem rawNewsItem,
            String captureMode,
            String licensePolicy,
            String licenseNote,
            String errorMessage
    ) {
        return new RawNewsContent(
                rawNewsItem,
                captureMode,
                licensePolicy,
                licenseNote,
                "FAILED",
                null,
                null,
                errorMessage
        );
    }

    public Long getId() {
        return id;
    }

    public RawNewsItem getRawNewsItem() {
        return rawNewsItem;
    }

    public String getCaptureMode() {
        return captureMode;
    }

    public String getContentText() {
        return contentText;
    }

    public String getContentTextHash() {
        return contentTextHash;
    }

    public String getLicensePolicy() {
        return licensePolicy;
    }

    public String getLicenseNote() {
        return licenseNote;
    }

    public String getFetchStatus() {
        return fetchStatus;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public LocalDateTime getFetchedAt() {
        return fetchedAt;
    }
}
