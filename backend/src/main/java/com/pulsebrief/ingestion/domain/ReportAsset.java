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

    @Column(name = "asset_status")
    private String assetStatus;

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
        this.createdAt = now;
        this.updatedAt = now;
    }

    public Long getId() {
        return id;
    }

    public CandidateArticle getCandidateArticle() {
        return candidateArticle;
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

    public String getAssetStatus() {
        return assetStatus;
    }
}
