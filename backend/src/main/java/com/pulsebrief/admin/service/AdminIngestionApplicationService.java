package com.pulsebrief.admin.service;

import com.pulsebrief.admin.api.AdminIngestionAnomalyResponse;
import com.pulsebrief.admin.api.AdminIngestionJobResponse;
import com.pulsebrief.admin.api.AdminIngestionMetricsResponse;
import com.pulsebrief.admin.api.AdminIngestionRunRequest;
import com.pulsebrief.admin.api.AdminIngestionRunResponse;
import com.pulsebrief.admin.api.AdminIngestionSourceResponse;
import com.pulsebrief.admin.api.AdminIngestionScheduleRequest;
import com.pulsebrief.article.repository.ArticleRepository;
import com.pulsebrief.common.api.PageResponse;
import com.pulsebrief.ingestion.domain.NewsIngestionJob;
import com.pulsebrief.ingestion.domain.NewsIngestionSource;
import com.pulsebrief.ingestion.repository.CandidateArticleRepository;
import com.pulsebrief.ingestion.repository.NewsIngestionJobRepository;
import com.pulsebrief.ingestion.repository.NewsIngestionSourceRepository;
import com.pulsebrief.ingestion.service.NewsIngestionRunService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
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
    private final NewsIngestionRunService newsIngestionRunService;
    private final JdbcTemplate jdbcTemplate;

    public AdminIngestionApplicationService(
            NewsIngestionJobRepository jobRepository,
            NewsIngestionSourceRepository sourceRepository,
            CandidateArticleRepository candidateArticleRepository,
            ArticleRepository articleRepository,
            NewsIngestionRunService newsIngestionRunService,
            JdbcTemplate jdbcTemplate
    ) {
        this.jobRepository = jobRepository;
        this.sourceRepository = sourceRepository;
        this.candidateArticleRepository = candidateArticleRepository;
        this.articleRepository = articleRepository;
        this.newsIngestionRunService = newsIngestionRunService;
        this.jdbcTemplate = jdbcTemplate;
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
    public PageResponse<AdminIngestionAnomalyResponse> listAnomalies(Integer page, Integer pageSize) {
        int safePage = Math.max(page == null ? 1 : page, 1);
        int safePageSize = Math.min(Math.max(pageSize == null ? 20 : pageSize, 1), 50);
        int offset = (safePage - 1) * safePageSize;
        Long total = jdbcTemplate.queryForObject("select count(*) from (" + anomalySelectSql() + ") anomaly_count", Long.class);
        List<AdminIngestionAnomalyResponse> items = jdbcTemplate.query(
                "select * from (" + anomalySelectSql() + ") anomaly_list "
                        + "order by severity_rank asc, fetched_at desc, raw_news_item_id desc "
                        + "limit ? offset ?",
                anomalyRowMapper(),
                safePageSize,
                offset
        );
        return PageResponse.of(items, safePage, safePageSize, total == null ? 0 : total);
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

    public AdminIngestionRunResponse runSource(Long id, AdminIngestionRunRequest request) {
        NewsIngestionSource source = sourceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Ingestion source not found"));
        return newsIngestionRunService.runManual(
                source,
                request.pageSizeOrDefault(),
                request.shouldGenerateCandidates()
        );
    }

    @Transactional
    public AdminIngestionSourceResponse updateSourceSchedule(Long id, AdminIngestionScheduleRequest request) {
        if (request == null || request.enabled() == null) {
            throw new ResponseStatusException(UNPROCESSABLE_ENTITY, "Schedule enabled value is required");
        }
        int intervalMinutes = request.intervalMinutes() == null ? 60 : request.intervalMinutes();
        if (intervalMinutes < 5 || intervalMinutes > 1440) {
            throw new ResponseStatusException(UNPROCESSABLE_ENTITY, "Schedule interval must be between 5 and 1440 minutes");
        }
        NewsIngestionSource source = sourceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Ingestion source not found"));
        source.configureSchedule(request.enabled(), intervalMinutes);
        return toSourceResponse(source);
    }

    @Transactional
    public AdminIngestionJobResponse cancelJob(Long id) {
        NewsIngestionJob job = jobRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Ingestion job not found"));
        job.requestCancel();
        return toJobResponse(job);
    }

    private AdminIngestionJobResponse toJobResponse(NewsIngestionJob job) {
        return new AdminIngestionJobResponse(
                job.getId(),
                job.getSourceCode(),
                job.getTriggerType(),
                job.getJobStatus(),
                job.getAttemptCount(),
                job.getMaxAttempts(),
                job.getNextRetryAt(),
                job.isCancelRequested(),
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
                source.isScheduleEnabled(),
                source.getScheduleIntervalMinutes(),
                source.getNextRunAt(),
                source.getContentAccessPolicy(),
                source.getMaxAgeHours(),
                source.isPdfDownloadAllowed(),
                source.isFullTextAllowed()
        );
    }

    private String anomalySelectSql() {
        return """
                select
                    id,
                    id as raw_news_item_id,
                    title,
                    source_code,
                    source_name,
                    original_url,
                    published_at,
                    fetched_at,
                    'MISSING_ORIGINAL_URL' as issue_type,
                    'HIGH' as severity,
                    1 as severity_rank,
                    '原始资讯缺少原文链接，无法满足可追溯要求' as description
                from raw_news_item
                where trim(coalesce(original_url, '')) = ''
                union all
                select
                    id,
                    id as raw_news_item_id,
                    title,
                    source_code,
                    source_name,
                    original_url,
                    published_at,
                    fetched_at,
                    'PUBLISHED_AT_IN_FUTURE' as issue_type,
                    'HIGH' as severity,
                    1 as severity_rank,
                    '原始资讯发布时间晚于当前时间，可能来自来源时区或解析错误' as description
                from raw_news_item
                where published_at > date_add(now(), interval 10 minute)
                union all
                select
                    id,
                    id as raw_news_item_id,
                    title,
                    source_code,
                    source_name,
                    original_url,
                    published_at,
                    fetched_at,
                    'MISSING_SOURCE' as issue_type,
                    'MEDIUM' as severity,
                    2 as severity_rank,
                    '原始资讯缺少来源名称，影响用户端可信展示' as description
                from raw_news_item
                where trim(coalesce(source_name, '')) = ''
                union all
                select
                    id,
                    id as raw_news_item_id,
                    title,
                    source_code,
                    source_name,
                    original_url,
                    published_at,
                    fetched_at,
                    'PUBLISHED_AT_MISSING' as issue_type,
                    'MEDIUM' as severity,
                    2 as severity_rank,
                    '原始资讯缺少发布时间，影响最新内容筛选与排序' as description
                from raw_news_item
                where published_at is null
                """;
    }

    private RowMapper<AdminIngestionAnomalyResponse> anomalyRowMapper() {
        return (rs, rowNum) -> new AdminIngestionAnomalyResponse(
                rs.getLong("id"),
                rs.getLong("raw_news_item_id"),
                rs.getString("title"),
                rs.getString("source_code"),
                rs.getString("source_name"),
                rs.getString("original_url"),
                rs.getTimestamp("published_at") == null ? null : rs.getTimestamp("published_at").toLocalDateTime(),
                rs.getTimestamp("fetched_at") == null ? null : rs.getTimestamp("fetched_at").toLocalDateTime(),
                rs.getString("issue_type"),
                rs.getString("severity"),
                rs.getString("description")
        );
    }
}
