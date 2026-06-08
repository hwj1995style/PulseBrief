package com.pulsebrief.digest.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "daily_digest")
public class DailyDigest {
    @Id
    private Long id;

    @Column(name = "digest_date")
    private LocalDate digestDate;

    @Column(name = "digest_type")
    private String digestType;

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

    protected DailyDigest() {
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
}
