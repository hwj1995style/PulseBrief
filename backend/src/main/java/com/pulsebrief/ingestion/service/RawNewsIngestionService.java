package com.pulsebrief.ingestion.service;

import com.pulsebrief.ingestion.domain.NewsIngestionJob;
import com.pulsebrief.ingestion.domain.RawNewsItem;
import com.pulsebrief.ingestion.provider.RawNewsPayload;
import com.pulsebrief.ingestion.repository.NewsIngestionJobRepository;
import com.pulsebrief.ingestion.repository.RawNewsItemRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RawNewsIngestionService {
    private final RawNewsItemRepository rawNewsItemRepository;
    private final NewsIngestionJobRepository jobRepository;
    private final IngestionDeduplicationService deduplicationService;

    public RawNewsIngestionService(
            RawNewsItemRepository rawNewsItemRepository,
            NewsIngestionJobRepository jobRepository,
            IngestionDeduplicationService deduplicationService
    ) {
        this.rawNewsItemRepository = rawNewsItemRepository;
        this.jobRepository = jobRepository;
        this.deduplicationService = deduplicationService;
    }

    @Transactional
    public IngestionResult ingest(String sourceCode, String triggerType, List<RawNewsPayload> payloads) {
        NewsIngestionJob job = jobRepository.save(new NewsIngestionJob(sourceCode, triggerType));
        int fetchedCount = payloads.size();
        int newCount = 0;
        int duplicateCount = 0;

        for (RawNewsPayload payload : payloads) {
            String originalUrlHash = deduplicationService.urlHash(payload.originalUrl());
            String contentHash = deduplicationService.contentHash(
                    payload.sourceName(),
                    payload.title(),
                    payload.publishedAt()
            );
            if (rawNewsItemRepository.existsByOriginalUrlHash(originalUrlHash)
                    || rawNewsItemRepository.existsBySourceCodeAndContentHash(sourceCode, contentHash)) {
                duplicateCount++;
                continue;
            }

            rawNewsItemRepository.save(new RawNewsItem(sourceCode, payload, originalUrlHash, contentHash));
            newCount++;
        }

        job.complete(fetchedCount, newCount, duplicateCount, 0);
        jobRepository.save(job);
        return new IngestionResult(job.getId(), fetchedCount, newCount, duplicateCount, 0);
    }
}
