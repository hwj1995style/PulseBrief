package com.pulsebrief.ingestion.service;

public interface AiSummaryProvider {
    String providerType();

    AiSummaryProviderResult generate(AiSummaryRequest request);
}
