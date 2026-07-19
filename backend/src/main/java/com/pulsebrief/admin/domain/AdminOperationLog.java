package com.pulsebrief.admin.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "admin_operation_log")
public class AdminOperationLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "operation_module")
    private String operationModule;

    @Column(name = "action_type")
    private String actionType;

    @Column(name = "target_type")
    private String targetType;

    @Column(name = "target_id")
    private Long targetId;

    @Column(name = "target_title")
    private String targetTitle;

    @Column(name = "operation_status")
    private String operationStatus;

    @Column(name = "operator_user_id")
    private Long operatorUserId;

    @Column(name = "operator_name")
    private String operatorName;

    @Column(name = "operator_role")
    private String operatorRole;

    private String detail;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    protected AdminOperationLog() {
    }

    public AdminOperationLog(
            String operationModule,
            String actionType,
            String targetType,
            Long targetId,
            String targetTitle,
            String operationStatus,
            Long operatorUserId,
            String operatorName,
            String operatorRole,
            String detail
    ) {
        this.operationModule = operationModule;
        this.actionType = actionType;
        this.targetType = targetType;
        this.targetId = targetId;
        this.targetTitle = targetTitle;
        this.operationStatus = operationStatus;
        this.operatorUserId = operatorUserId;
        this.operatorName = operatorName;
        this.operatorRole = operatorRole;
        this.detail = detail;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getOperationModule() {
        return operationModule;
    }

    public String getActionType() {
        return actionType;
    }

    public String getTargetType() {
        return targetType;
    }

    public Long getTargetId() {
        return targetId;
    }

    public String getTargetTitle() {
        return targetTitle;
    }

    public String getOperationStatus() {
        return operationStatus;
    }

    public Long getOperatorUserId() {
        return operatorUserId;
    }

    public String getOperatorName() {
        return operatorName;
    }

    public String getOperatorRole() {
        return operatorRole;
    }

    public String getDetail() {
        return detail;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
