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
@Table(name = "report_asset")
public class ReportAsset {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "candidate_article_id")
    private CandidateArticle candidateArticle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_file_id")
    private ReportAssetFile assetFile;

    @Column(name = "source_code")
    private String sourceCode;

    private String title;

    @Column(name = "original_url")
    private String originalUrl;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "file_hash")
    private String fileHash;

    @Column(name = "license_policy")
    private String licensePolicy;

    @Column(name = "license_note")
    private String licenseNote;

    @Column(name = "asset_status")
    private String assetStatus;

    @Column(name = "cache_status")
    private String cacheStatus;

    @Column(name = "cache_error_message")
    private String cacheErrorMessage;

    @Column(name = "cache_requested_at")
    private LocalDateTime cacheRequestedAt;

    @Column(name = "cache_completed_at")
    private LocalDateTime cacheCompletedAt;

    @Column(name = "review_note")
    private String reviewNote;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "reviewed_by")
    private String reviewedBy;

    @Column(name = "downloaded_at")
    private LocalDateTime downloadedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    protected ReportAsset() {
    }

    public ReportAsset(
            CandidateArticle candidateArticle,
            String sourceCode,
            String title,
            String originalUrl,
            String fileName,
            Long fileSizeBytes,
            String fileHash,
            String licensePolicy
    ) {
        LocalDateTime now = LocalDateTime.now();
        this.candidateArticle = candidateArticle;
        this.sourceCode = sourceCode;
        this.title = title;
        this.originalUrl = originalUrl;
        this.fileName = fileName;
        this.fileSizeBytes = fileSizeBytes;
        this.fileHash = fileHash;
        this.licensePolicy = licensePolicy;
        this.assetStatus = "PENDING_REVIEW";
        this.cacheStatus = "NOT_CACHED";
        this.createdAt = now;
        this.updatedAt = now;
    }

    public Long getId() {
        return id;
    }

    public CandidateArticle getCandidateArticle() {
        return candidateArticle;
    }

    public ReportAssetFile getAssetFile() {
        return assetFile;
    }

    public String getSourceCode() {
        return sourceCode;
    }

    public String getFileHash() {
        return fileHash;
    }

    public String getTitle() {
        return title;
    }

    public String getOriginalUrl() {
        return originalUrl;
    }

    public String getFileName() {
        return fileName;
    }

    public Long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public String getLicensePolicy() {
        return licensePolicy;
    }

    public String getLicenseNote() {
        return licenseNote;
    }

    public String getAssetStatus() {
        return assetStatus;
    }

    public String getCacheStatus() {
        return cacheStatus == null ? "NOT_CACHED" : cacheStatus;
    }

    public String getCacheErrorMessage() {
        return cacheErrorMessage;
    }

    public LocalDateTime getCacheCompletedAt() {
        return cacheCompletedAt;
    }

    public String getReviewNote() {
        return reviewNote;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public String getReviewedBy() {
        return reviewedBy;
    }

    public void markCachePending() {
        this.cacheStatus = "PENDING";
        this.cacheErrorMessage = null;
        this.cacheRequestedAt = LocalDateTime.now();
        this.updatedAt = this.cacheRequestedAt;
    }

    public void markCacheSuccess(ReportAssetFile assetFile, String licenseNote) {
        LocalDateTime now = LocalDateTime.now();
        this.assetFile = assetFile;
        this.licenseNote = licenseNote;
        this.cacheStatus = "SUCCESS";
        this.cacheErrorMessage = null;
        this.cacheCompletedAt = now;
        this.downloadedAt = assetFile.getDownloadedAt();
        this.fileSizeBytes = assetFile.getFileSizeBytes();
        this.updatedAt = now;
    }

    public void markCacheSkipped(String message, String licenseNote) {
        LocalDateTime now = LocalDateTime.now();
        this.licenseNote = licenseNote;
        this.cacheStatus = "SKIPPED";
        this.cacheErrorMessage = message;
        this.cacheCompletedAt = now;
        this.updatedAt = now;
    }

    public void markCacheFailed(String message, String licenseNote) {
        LocalDateTime now = LocalDateTime.now();
        this.licenseNote = licenseNote;
        this.cacheStatus = "FAILED";
        this.cacheErrorMessage = message;
        this.cacheCompletedAt = now;
        this.updatedAt = now;
    }

    public void approve(String reviewNote, String reviewedBy) {
        LocalDateTime now = LocalDateTime.now();
        this.assetStatus = "APPROVED";
        this.reviewNote = reviewNote;
        this.reviewedBy = reviewedBy;
        this.reviewedAt = now;
        this.updatedAt = now;
    }

    public void reject(String reviewNote, String reviewedBy) {
        LocalDateTime now = LocalDateTime.now();
        this.assetStatus = "REJECTED";
        this.reviewNote = reviewNote;
        this.reviewedBy = reviewedBy;
        this.reviewedAt = now;
        this.updatedAt = now;
    }
}
