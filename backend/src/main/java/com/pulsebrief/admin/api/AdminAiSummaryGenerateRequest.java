package com.pulsebrief.admin.api;

public record AdminAiSummaryGenerateRequest(
        String inputSourceType,
        String providerType,
        String promptVersion
) {
}
