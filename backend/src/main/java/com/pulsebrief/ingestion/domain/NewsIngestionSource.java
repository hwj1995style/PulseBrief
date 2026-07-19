package com.pulsebrief.ingestion.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "news_ingestion_source")
public class NewsIngestionSource {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String code;

    private String name;

    @Column(name = "provider_type")
    private String providerType;

    @Column(name = "base_url")
    private String baseUrl;

    @Column(name = "default_category_code")
    private String defaultCategoryCode;

    private Byte enabled;

    @Column(name = "schedule_enabled")
    private Byte scheduleEnabled;

    @Column(name = "schedule_interval_minutes")
    private Integer scheduleIntervalMinutes;

    @Column(name = "next_run_at")
    private LocalDateTime nextRunAt;

    @Column(name = "rate_limit_per_hour")
    private Integer rateLimitPerHour;

    @Column(name = "content_access_policy")
    private String contentAccessPolicy;

    @Column(name = "max_age_hours")
    private Integer maxAgeHours;

    @Column(name = "allow_pdf_download")
    private Byte allowPdfDownload;

    @Column(name = "allow_full_text")
    private Byte allowFullText;

    @Column(name = "license_note")
    private String licenseNote;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    protected NewsIngestionSource() {
    }

    public static NewsIngestionSource fixture(
            String code,
            String name,
            String contentAccessPolicy,
            Integer maxAgeHours
    ) {
        LocalDateTime now = LocalDateTime.now();
        NewsIngestionSource source = new NewsIngestionSource();
        source.code = code;
        source.name = name;
        source.providerType = "FIXTURE";
        source.baseUrl = "fixture://" + code;
        source.defaultCategoryCode = "global";
        source.enabled = 1;
        source.scheduleEnabled = 0;
        source.scheduleIntervalMinutes = 60;
        source.rateLimitPerHour = 60;
        source.contentAccessPolicy = contentAccessPolicy;
        source.maxAgeHours = maxAgeHours;
        source.allowPdfDownload = "PDF_ALLOWED".equals(contentAccessPolicy) ? (byte) 1 : (byte) 0;
        source.allowFullText = "FULLTEXT_ALLOWED".equals(contentAccessPolicy) ? (byte) 1 : (byte) 0;
        source.licenseNote = "Fixture source for ingestion tests";
        source.createdAt = now;
        source.updatedAt = now;
        return source;
    }

    public String getContentAccessPolicy() {
        return contentAccessPolicy;
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getProviderType() {
        return providerType;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getDefaultCategoryCode() {
        return defaultCategoryCode;
    }

    public boolean isScheduleEnabled() {
        return scheduleEnabled != null && scheduleEnabled == 1;
    }

    public Integer getScheduleIntervalMinutes() {
        return scheduleIntervalMinutes == null ? 60 : scheduleIntervalMinutes;
    }

    public LocalDateTime getNextRunAt() {
        return nextRunAt;
    }

    public void configureSchedule(boolean enabled, int intervalMinutes) {
        this.scheduleEnabled = enabled ? (byte) 1 : (byte) 0;
        this.scheduleIntervalMinutes = intervalMinutes;
        this.nextRunAt = enabled ? LocalDateTime.now() : null;
        this.updatedAt = LocalDateTime.now();
    }

    public void scheduleNextRun() {
        this.nextRunAt = LocalDateTime.now().plusMinutes(getScheduleIntervalMinutes());
        this.updatedAt = LocalDateTime.now();
    }

    public Integer getRateLimitPerHour() {
        return rateLimitPerHour;
    }

    public boolean isEnabled() {
        return enabled != null && enabled == 1;
    }

    public void updateEnabled(boolean enabled) {
        this.enabled = enabled ? (byte) 1 : (byte) 0;
        this.updatedAt = LocalDateTime.now();
    }

    public Integer getMaxAgeHours() {
        return maxAgeHours;
    }

    public boolean isPdfDownloadAllowed() {
        return allowPdfDownload != null && allowPdfDownload == 1;
    }

    public boolean isFullTextAllowed() {
        return allowFullText != null && allowFullText == 1;
    }

    public String getLicenseNote() {
        return licenseNote;
    }
}
