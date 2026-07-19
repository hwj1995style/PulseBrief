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

    @Column(name = "attempt_count")
    private Integer attemptCount;

    @Column(name = "max_attempts")
    private Integer maxAttempts;

    @Column(name = "next_retry_at")
    private LocalDateTime nextRetryAt;

    @Column(name = "cancel_requested")
    private Byte cancelRequested;

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
        this(sourceCode, triggerType, 1);
    }

    public NewsIngestionJob(String sourceCode, String triggerType, int maxAttempts) {
        LocalDateTime now = LocalDateTime.now();
        this.sourceCode = sourceCode;
        this.triggerType = triggerType;
        this.jobStatus = "RUNNING";
        this.attemptCount = 1;
        this.maxAttempts = Math.max(maxAttempts, 1);
        this.cancelRequested = 0;
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

    public void waitForRetry(String errorMessage, int delayMinutes) {
        this.jobStatus = "WAITING_RETRY";
        this.errorMessage = errorMessage;
        this.nextRetryAt = LocalDateTime.now().plusMinutes(Math.max(delayMinutes, 1));
        this.finishedAt = null;
        this.updatedAt = LocalDateTime.now();
    }

    public void startRetry() {
        if (isCancelRequested()) {
            this.jobStatus = "CANCELLED";
            this.finishedAt = LocalDateTime.now();
            this.nextRetryAt = null;
            return;
        }
        this.jobStatus = "RUNNING";
        this.attemptCount = getAttemptCount() + 1;
        this.startedAt = LocalDateTime.now();
        this.finishedAt = null;
        this.nextRetryAt = null;
        this.updatedAt = this.startedAt;
    }

    public void requestCancel() {
        this.cancelRequested = 1;
        if ("WAITING_RETRY".equals(this.jobStatus)) {
            this.jobStatus = "CANCELLED";
            this.finishedAt = LocalDateTime.now();
            this.nextRetryAt = null;
        }
        this.updatedAt = LocalDateTime.now();
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

    public Integer getAttemptCount() { return attemptCount == null ? 1 : attemptCount; }
    public Integer getMaxAttempts() { return maxAttempts == null ? 1 : maxAttempts; }
    public LocalDateTime getNextRetryAt() { return nextRetryAt; }
    public boolean isCancelRequested() { return cancelRequested != null && cancelRequested == 1; }
    public boolean canRetry() { return getAttemptCount() < getMaxAttempts() && !isCancelRequested(); }
}
