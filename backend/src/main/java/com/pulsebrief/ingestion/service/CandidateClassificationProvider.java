package com.pulsebrief.ingestion.service;

import com.pulsebrief.ingestion.domain.RawNewsItem;
import java.util.Optional;

public interface CandidateClassificationProvider {
    String providerType();

    Optional<ClassificationDecision> classify(RawNewsItem rawItem);
}
