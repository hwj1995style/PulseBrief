package com.pulsebrief.ingestion.service;

import com.pulsebrief.admin.api.AdminIngestionRunResponse;
import com.pulsebrief.ingestion.domain.NewsIngestionJob;
import com.pulsebrief.ingestion.domain.NewsIngestionSource;
import com.pulsebrief.ingestion.provider.NewsIngestionProvider;
import com.pulsebrief.ingestion.provider.RawNewsPayload;
import com.pulsebrief.ingestion.repository.NewsIngestionJobRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;

@Service
public class NewsIngestionRunService {
    private static final String MANUAL = "MANUAL";

    private final List<NewsIngestionProvider> providers;
    private final NewsIngestionJobRepository jobRepository;
    private final RawNewsIngestionService rawNewsIngestionService;
    private final CandidateArticleGenerationService candidateArticleGenerationService;

    public NewsIngestionRunService(
            List<NewsIngestionProvider> providers,
            NewsIngestionJobRepository jobRepository,
            RawNewsIngestionService rawNewsIngestionService,
            CandidateArticleGenerationService candidateArticleGenerationService
    ) {
        this.providers = providers;
        this.jobRepository = jobRepository;
        this.rawNewsIngestionService = rawNewsIngestionService;
        this.candidateArticleGenerationService = candidateArticleGenerationService;
    }

    public AdminIngestionRunResponse runManual(
            NewsIngestionSource source,
            int pageSize,
            boolean generateCandidates
    ) {
        if (!source.isEnabled()) {
            throw new ResponseStatusException(UNPROCESSABLE_ENTITY, "Ingestion source is disabled");
        }
        enforceRateLimit(source);

        NewsIngestionJob job = jobRepository.save(new NewsIngestionJob(source.getCode(), MANUAL));
        try {
            NewsIngestionProvider provider = providerFor(source.getProviderType());
            IngestionRequest request = new IngestionRequest(
                    source.getCode(),
                    List.of(),
                    "en",
                    "US",
                    source.getDefaultCategoryCode(),
                    pageSize
            );
            List<RawNewsPayload> payloads = provider.fetch(source, request);
            RawNewsIngestionCounts counts = rawNewsIngestionService.ingestPayloads(source.getCode(), payloads);
            int candidateCount = generateCandidates
                    ? candidateArticleGenerationService.generatePendingCandidates(source.getCode(), pageSize).generatedCount()
                    : 0;
            job.complete(counts.fetchedCount(), counts.newCount(), counts.duplicateCount(), candidateCount);
            jobRepository.save(job);
            return toResponse(job, source);
        } catch (ResponseStatusException e) {
            String message = e.getReason() == null ? e.getMessage() : e.getReason();
            job.fail(message);
            jobRepository.save(job);
            throw e;
        } catch (RuntimeException e) {
            String message = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            job.fail(message);
            jobRepository.save(job);
            throw new ResponseStatusException(BAD_GATEWAY, message, e);
        }
    }

    private NewsIngestionProvider providerFor(String providerType) {
        return providers.stream()
                .filter(provider -> provider.providerType().equalsIgnoreCase(providerType))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        UNPROCESSABLE_ENTITY,
                        "Ingestion provider is not registered: " + providerType
                ));
    }

    private void enforceRateLimit(NewsIngestionSource source) {
        Integer rateLimitPerHour = source.getRateLimitPerHour();
        if (rateLimitPerHour == null || rateLimitPerHour <= 0) {
            return;
        }
        long recentRuns = jobRepository.countBySourceCodeAndStartedAtGreaterThanEqual(
                source.getCode(),
                LocalDateTime.now().minusHours(1)
        );
        if (recentRuns >= rateLimitPerHour) {
            throw new ResponseStatusException(
                    UNPROCESSABLE_ENTITY,
                    "Ingestion source rate limit reached"
            );
        }
    }

    private AdminIngestionRunResponse toResponse(NewsIngestionJob job, NewsIngestionSource source) {
        return new AdminIngestionRunResponse(
                job.getId(),
                source.getCode(),
                source.getProviderType(),
                job.getJobStatus(),
                job.getFetchedCount(),
                job.getNewCount(),
                job.getDuplicateCount(),
                job.getCandidateCount(),
                job.getErrorMessage()
        );
    }
}
