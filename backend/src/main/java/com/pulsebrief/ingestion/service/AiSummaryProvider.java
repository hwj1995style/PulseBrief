package com.pulsebrief.ingestion.service;

public interface AiSummaryProvider {
    String providerType();

    String modelName();

    AiSummaryProviderResult generate(AiSummaryRequest request);
}
