package com.pulsebrief.admin.api;

import java.util.List;

public record AdminAiSummaryTaskResponse(
        Long id,
        String status,
        String inputSourceType,
        Long inputRefId,
        String inputPreview,
        String providerType,
        String modelName,
        String promptVersion,
        String generatedSummary,
        List<String> generatedKeyPoints,
        String generatedImpactAnalysis,
        String errorMessage,
        String requestedBy,
        String startedAt,
        String finishedAt
) {
}
