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
@Table(name = "ai_summary_task")
public class AiSummaryTask {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_article_id")
    private CandidateArticle candidateArticle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "raw_news_item_id")
    private RawNewsItem rawNewsItem;

    @Column(name = "input_source_type")
    private String inputSourceType;

    @Column(name = "input_ref_id")
    private Long inputRefId;

    @Column(name = "input_hash")
    private String inputHash;

    @Column(name = "input_preview")
    private String inputPreview;

    @Column(name = "provider_type")
    private String providerType;

    @Column(name = "model_name")
    private String modelName;

    @Column(name = "prompt_version")
    private String promptVersion;

    @Column(name = "task_status")
    private String taskStatus;

    @Column(name = "generated_summary")
    private String generatedSummary;

    @Column(name = "generated_key_points")
    private String generatedKeyPoints;

    @Column(name = "generated_impact_analysis")
    private String generatedImpactAnalysis;

    @Column(name = "token_prompt_count")
    private Integer tokenPromptCount;

    @Column(name = "token_completion_count")
    private Integer tokenCompletionCount;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "requested_by")
    private String requestedBy;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    protected AiSummaryTask() {
    }

    public AiSummaryTask(
            CandidateArticle candidateArticle,
            String inputSourceType,
            Long inputRefId,
            String inputHash,
            String inputPreview,
            String providerType,
            String modelName,
            String promptVersion,
            String requestedBy
    ) {
        LocalDateTime now = LocalDateTime.now();
        this.candidateArticle = candidateArticle;
        this.rawNewsItem = candidateArticle.getRawNewsItem();
        this.inputSourceType = inputSourceType;
        this.inputRefId = inputRefId;
        this.inputHash = inputHash;
        this.inputPreview = inputPreview;
        this.providerType = providerType;
        this.modelName = modelName;
        this.promptVersion = promptVersion;
        this.taskStatus = "PENDING";
        this.requestedBy = requestedBy;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public Long getId() {
        return id;
    }

    public CandidateArticle getCandidateArticle() {
        return candidateArticle;
    }

    public RawNewsItem getRawNewsItem() {
        return rawNewsItem;
    }

    public String getInputSourceType() {
        return inputSourceType;
    }

    public Long getInputRefId() {
        return inputRefId;
    }

    public String getInputHash() {
        return inputHash;
    }

    public String getInputPreview() {
        return inputPreview;
    }

    public String getProviderType() {
        return providerType;
    }

    public String getModelName() {
        return modelName;
    }

    public String getPromptVersion() {
        return promptVersion;
    }

    public String getTaskStatus() {
        return taskStatus;
    }

    public String getGeneratedSummary() {
        return generatedSummary;
    }

    public String getGeneratedKeyPoints() {
        return generatedKeyPoints;
    }

    public String getGeneratedImpactAnalysis() {
        return generatedImpactAnalysis;
    }

    public Integer getTokenPromptCount() {
        return tokenPromptCount;
    }

    public Integer getTokenCompletionCount() {
        return tokenCompletionCount;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getRequestedBy() {
        return requestedBy;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public LocalDateTime getFinishedAt() {
        return finishedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void markRunning() {
        LocalDateTime now = LocalDateTime.now();
        this.taskStatus = "RUNNING";
        this.startedAt = now;
        this.updatedAt = now;
    }

    public void markSuccess(
            String generatedSummary,
            String generatedKeyPoints,
            String generatedImpactAnalysis,
            Integer tokenPromptCount,
            Integer tokenCompletionCount
    ) {
        LocalDateTime now = LocalDateTime.now();
        this.taskStatus = "SUCCESS";
        this.generatedSummary = generatedSummary;
        this.generatedKeyPoints = generatedKeyPoints;
        this.generatedImpactAnalysis = generatedImpactAnalysis;
        this.tokenPromptCount = tokenPromptCount;
        this.tokenCompletionCount = tokenCompletionCount;
        this.errorMessage = null;
        this.finishedAt = now;
        this.updatedAt = now;
    }

    public void markSkipped(String errorMessage) {
        LocalDateTime now = LocalDateTime.now();
        this.taskStatus = "SKIPPED";
        this.errorMessage = errorMessage;
        this.finishedAt = now;
        this.updatedAt = now;
    }

    public void markFailed(String errorMessage) {
        LocalDateTime now = LocalDateTime.now();
        this.taskStatus = "FAILED";
        this.errorMessage = errorMessage;
        this.finishedAt = now;
        this.updatedAt = now;
    }
}
