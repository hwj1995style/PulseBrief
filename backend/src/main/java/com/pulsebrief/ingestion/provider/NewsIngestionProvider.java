package com.pulsebrief.ingestion.provider;

import com.pulsebrief.ingestion.service.IngestionRequest;
import java.util.List;

public interface NewsIngestionProvider {
    String providerType();

    List<RawNewsPayload> fetch(IngestionRequest request);
}
