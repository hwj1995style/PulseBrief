package com.pulsebrief.admin.service;

import com.pulsebrief.admin.api.AdminIngestionJobResponse;
import com.pulsebrief.admin.api.AdminIngestionMetricsResponse;
import com.pulsebrief.admin.api.AdminIngestionSourceResponse;
import com.pulsebrief.article.repository.ArticleRepository;
import com.pulsebrief.common.api.PageResponse;
import com.pulsebrief.ingestion.domain.NewsIngestionJob;
import com.pulsebrief.ingestion.domain.NewsIngestionSource;
import com.pulsebrief.ingestion.repository.CandidateArticleRepository;
import com.pulsebrief.ingestion.repository.NewsIngestionJobRepository;
import com.pulsebrief.ingestion.repository.NewsIngestionSourceRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;

@Service
public class AdminIngestionApplicationService {
    private static final String PUBLISHED = "PUBLISHED";
    private static final String FAILED = "FAILED";

    private final NewsIngestionJobRepository jobRepository;
    private final NewsIngestionSourceRepository sourceRepository;
    private final CandidateArticleRepository candidateArticleRepository;
    private final ArticleRepository articleRepository;

    public AdminIngestionApplicationService(
            NewsIngestionJobRepository jobRepository,
            NewsIngestionSourceRepository sourceRepository,
            CandidateArticleRepository candidateArticleRepository,
            ArticleRepository articleRepository
    ) {
        this.jobRepository = jobRepository;
        this.sourceRepository = sourceRepository;
        this.candidateArticleRepository = candidateArticleRepository;
        this.articleRepository = articleRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminIngestionJobResponse> listJobs(String status, Integer page, Integer pageSize) {
        int safePage = Math.max(page == null ? 1 : page, 1);
        int safePageSize = Math.min(Math.max(pageSize == null ? 20 : pageSize, 1), 50);
        PageRequest pageable = PageRequest.of(safePage - 1, safePageSize);
        Page<NewsIngestionJob> jobs = status == null || status.isBlank()
                ? jobRepository.findAllByOrderByStartedAtDescIdDesc(pageable)
                : jobRepository.findByJobStatusOrderByStartedAtDescIdDesc(status.trim().toUpperCase(), pageable);
        return PageResponse.of(
                jobs.getContent().stream().map(this::toJobResponse).toList(),
                safePage,
                safePageSize,
                jobs.getTotalElements()
        );
    }

    @Transactional(readOnly = true)
    public AdminIngestionMetricsResponse todayMetrics() {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = start.plusDays(1);
        return new AdminIngestionMetricsResponse(
                jobRepository.sumFetchedCountByStartedAtBetween(start, end),
                candidateArticleRepository.countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(start, end),
                articleRepository.countByArticleStatusAndPublishTimeGreaterThanEqualAndPublishTimeLessThan(
                        PUBLISHED,
                        start,
                        end
                ),
                jobRepository.countByJobStatusAndStartedAtGreaterThanEqualAndStartedAtLessThan(FAILED, start, end)
        );
    }

    @Transactional(readOnly = true)
    public List<AdminIngestionSourceResponse> listSources() {
        return sourceRepository.findAllByOrderByCodeAsc()
                .stream()
                .map(this::toSourceResponse)
                .toList();
    }

    @Transactional
    public AdminIngestionSourceResponse updateSourceEnabled(Long id, Boolean enabled) {
        if (enabled == null) {
            throw new ResponseStatusException(UNPROCESSABLE_ENTITY, "Source enabled value is required");
        }
        NewsIngestionSource source = sourceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Ingestion source not found"));
        source.updateEnabled(enabled);
        return toSourceResponse(source);
    }

    private AdminIngestionJobResponse toJobResponse(NewsIngestionJob job) {
        return new AdminIngestionJobResponse(
                job.getId(),
                job.getSourceCode(),
                job.getTriggerType(),
                job.getJobStatus(),
                job.getStartedAt(),
                job.getFinishedAt(),
                job.getFetchedCount(),
                job.getNewCount(),
                job.getDuplicateCount(),
                job.getCandidateCount(),
                job.getErrorMessage()
        );
    }

    private AdminIngestionSourceResponse toSourceResponse(NewsIngestionSource source) {
        return new AdminIngestionSourceResponse(
                source.getId(),
                source.getCode(),
                source.getName(),
                source.getProviderType(),
                source.getDefaultCategoryCode(),
                source.isEnabled(),
                source.getContentAccessPolicy(),
                source.getMaxAgeHours(),
                source.isPdfDownloadAllowed(),
                source.isFullTextAllowed()
        );
    }
}
