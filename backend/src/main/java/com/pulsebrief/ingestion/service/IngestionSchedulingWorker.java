package com.pulsebrief.ingestion.service;

import com.pulsebrief.ingestion.domain.NewsIngestionJob;
import com.pulsebrief.ingestion.domain.NewsIngestionSource;
import com.pulsebrief.ingestion.repository.NewsIngestionJobRepository;
import com.pulsebrief.ingestion.repository.NewsIngestionSourceRepository;
import java.time.LocalDateTime;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(prefix = "pulsebrief.ingestion.scheduling", name = "enabled", havingValue = "true")
public class IngestionSchedulingWorker {
    private final NewsIngestionSourceRepository sourceRepository;
    private final NewsIngestionJobRepository jobRepository;
    private final NewsIngestionRunService runService;

    public IngestionSchedulingWorker(
            NewsIngestionSourceRepository sourceRepository,
            NewsIngestionJobRepository jobRepository,
            NewsIngestionRunService runService
    ) {
        this.sourceRepository = sourceRepository;
        this.jobRepository = jobRepository;
        this.runService = runService;
    }

    @Scheduled(fixedDelayString = "${pulsebrief.ingestion.scheduling.poll-interval-ms:30000}")
    @Transactional
    public void poll() {
        LocalDateTime now = LocalDateTime.now();
        for (NewsIngestionJob job : jobRepository
                .findTop20ByJobStatusAndNextRetryAtLessThanEqualOrderByNextRetryAtAsc("WAITING_RETRY", now)) {
            sourceRepository.findByCode(job.getSourceCode()).ifPresentOrElse(
                    source -> runService.retry(source, job),
                    () -> { job.fail("Ingestion source no longer exists"); jobRepository.save(job); }
            );
        }
        for (NewsIngestionSource source : sourceRepository
                .findTop20ByScheduleEnabledAndEnabledAndNextRunAtLessThanEqualOrderByNextRunAtAsc((byte) 1, (byte) 1, now)) {
            runService.runScheduled(source);
            source.scheduleNextRun();
            sourceRepository.save(source);
        }
    }
}
