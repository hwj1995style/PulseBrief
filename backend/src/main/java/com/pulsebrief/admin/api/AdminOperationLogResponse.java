package com.pulsebrief.admin.api;

import java.time.LocalDateTime;

public record AdminOperationLogResponse(
        Long id,
        String module,
        String actionType,
        String targetType,
        Long targetId,
        String targetTitle,
        String status,
        Long operatorUserId,
        String operatorName,
        String operatorRole,
        String detail,
        LocalDateTime createdAt
) {
}
