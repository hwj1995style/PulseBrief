package com.pulsebrief.ingestion.service;

public class AiUsageLimitExceededException extends RuntimeException {
    public AiUsageLimitExceededException(String message) {
        super(message);
    }
}
