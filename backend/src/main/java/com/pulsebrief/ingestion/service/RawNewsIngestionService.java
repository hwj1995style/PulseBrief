package com.pulsebrief.ingestion.service;

import com.pulsebrief.ingestion.domain.NewsIngestionJob;
import com.pulsebrief.ingestion.domain.NewsIngestionSource;
import com.pulsebrief.ingestion.domain.RawNewsItem;
import com.pulsebrief.ingestion.provider.RawNewsPayload;
import com.pulsebrief.ingestion.repository.NewsIngestionJobRepository;
import com.pulsebrief.ingestion.repository.NewsIngestionSourceRepository;
import com.pulsebrief.ingestion.repository.RawNewsItemRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RawNewsIngestionService {
    private final RawNewsItemRepository rawNewsItemRepository;
    private final NewsIngestionJobRepository jobRepository;
    private final NewsIngestionSourceRepository sourceRepository;
    private final IngestionDeduplicationService deduplicationService;

    public RawNewsIngestionService(
            RawNewsItemRepository rawNewsItemRepository,
            NewsIngestionJobRepository jobRepository,
            NewsIngestionSourceRepository sourceRepository,
            IngestionDeduplicationService deduplicationService
    ) {
        this.rawNewsItemRepository = rawNewsItemRepository;
        this.jobRepository = jobRepository;
        this.sourceRepository = sourceRepository;
        this.deduplicationService = deduplicationService;
    }

    @Transactional
    public IngestionResult ingest(String sourceCode, String triggerType, List<RawNewsPayload> payloads) {
        NewsIngestionJob job = jobRepository.save(new NewsIngestionJob(sourceCode, triggerType));
        Optional<NewsIngestionSource> source = sourceRepository.findByCode(sourceCode);
        int fetchedCount = payloads.size();
        int newCount = 0;
        int duplicateCount = 0;

        for (RawNewsPayload payload : payloads) {
            if (isOlderThanConfiguredWindow(source, payload)) {
                continue;
            }
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

    private boolean isOlderThanConfiguredWindow(Optional<NewsIngestionSource> source, RawNewsPayload payload) {
        if (source.isEmpty() || payload.publishedAt() == null) {
            return false;
        }
        Integer maxAgeHours = source.get().getMaxAgeHours();
        if (maxAgeHours == null || maxAgeHours <= 0) {
            return false;
        }
        return payload.publishedAt().isBefore(OffsetDateTime.now().minusHours(maxAgeHours));
    }
}
