package com.pulsebrief.ingestion.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_usage_event")
public class AiUsageEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "operation_type")
    private String operationType;

    @Column(name = "provider_type")
    private String providerType;

    @Column(name = "model_name")
    private String modelName;

    @Column(name = "request_status")
    private String requestStatus;

    @Column(name = "prompt_tokens")
    private Integer promptTokens;

    @Column(name = "completion_tokens")
    private Integer completionTokens;

    @Column(name = "estimated_cost_usd")
    private BigDecimal estimatedCostUsd;

    @Column(name = "error_code")
    private String errorCode;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    protected AiUsageEvent() {
    }

    public AiUsageEvent(String operationType, String providerType, String modelName, String status) {
        this.operationType = operationType;
        this.providerType = providerType;
        this.modelName = modelName;
        this.requestStatus = status;
        this.promptTokens = 0;
        this.completionTokens = 0;
        this.estimatedCostUsd = BigDecimal.ZERO;
        this.createdAt = LocalDateTime.now();
        this.completedAt = "RUNNING".equals(status) ? null : this.createdAt;
    }

    public void markSuccess(int promptTokens, int completionTokens, BigDecimal estimatedCostUsd) {
        this.requestStatus = "SUCCESS";
        this.promptTokens = Math.max(promptTokens, 0);
        this.completionTokens = Math.max(completionTokens, 0);
        this.estimatedCostUsd = estimatedCostUsd.max(BigDecimal.ZERO);
        this.completedAt = LocalDateTime.now();
    }

    public void markFailed(String errorCode) {
        this.requestStatus = "FAILED";
        this.errorCode = truncate(errorCode, 128);
        this.completedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getOperationType() { return operationType; }
    public String getProviderType() { return providerType; }
    public String getModelName() { return modelName; }
    public String getRequestStatus() { return requestStatus; }
    public Integer getPromptTokens() { return promptTokens; }
    public Integer getCompletionTokens() { return completionTokens; }
    public BigDecimal getEstimatedCostUsd() { return estimatedCostUsd; }
    public String getErrorCode() { return errorCode; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }

    private String truncate(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.substring(0, Math.min(trimmed.length(), maxLength));
    }
}
