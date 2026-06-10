package com.pulsebrief.ingestion.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "news_ingestion_job")
public class NewsIngestionJob {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_code")
    private String sourceCode;

    @Column(name = "trigger_type")
    private String triggerType;

    @Column(name = "job_status")
    private String jobStatus;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @Column(name = "fetched_count")
    private Integer fetchedCount;

    @Column(name = "new_count")
    private Integer newCount;

    @Column(name = "duplicate_count")
    private Integer duplicateCount;

    @Column(name = "candidate_count")
    private Integer candidateCount;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    protected NewsIngestionJob() {
    }

    public NewsIngestionJob(String sourceCode, String triggerType) {
        LocalDateTime now = LocalDateTime.now();
        this.sourceCode = sourceCode;
        this.triggerType = triggerType;
        this.jobStatus = "RUNNING";
        this.startedAt = now;
        this.fetchedCount = 0;
        this.newCount = 0;
        this.duplicateCount = 0;
        this.candidateCount = 0;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void complete(Integer fetchedCount, Integer newCount, Integer duplicateCount, Integer candidateCount) {
        this.jobStatus = "SUCCESS";
        this.fetchedCount = fetchedCount;
        this.newCount = newCount;
        this.duplicateCount = duplicateCount;
        this.candidateCount = candidateCount;
        this.finishedAt = LocalDateTime.now();
        this.updatedAt = this.finishedAt;
    }

    public void fail(String errorMessage) {
        this.jobStatus = "FAILED";
        this.errorMessage = errorMessage;
        this.finishedAt = LocalDateTime.now();
        this.updatedAt = this.finishedAt;
    }

    public Long getId() {
        return id;
    }

    public String getSourceCode() {
        return sourceCode;
    }

    public String getTriggerType() {
        return triggerType;
    }

    public String getJobStatus() {
        return jobStatus;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public LocalDateTime getFinishedAt() {
        return finishedAt;
    }

    public Integer getFetchedCount() {
        return fetchedCount;
    }

    public Integer getNewCount() {
        return newCount;
    }

    public Integer getDuplicateCount() {
        return duplicateCount;
    }

    public Integer getCandidateCount() {
        return candidateCount;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
