package com.pulsebrief.ingestion.repository;

import com.pulsebrief.ingestion.domain.NewsIngestionJob;
import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NewsIngestionJobRepository extends JpaRepository<NewsIngestionJob, Long> {
    Page<NewsIngestionJob> findAllByOrderByStartedAtDescIdDesc(Pageable pageable);

    Page<NewsIngestionJob> findByJobStatusOrderByStartedAtDescIdDesc(String jobStatus, Pageable pageable);

    long countByJobStatusAndStartedAtGreaterThanEqualAndStartedAtLessThan(
            String jobStatus,
            LocalDateTime start,
            LocalDateTime end
    );

    long countBySourceCodeAndStartedAtGreaterThanEqual(String sourceCode, LocalDateTime start);

    @Query("""
            select coalesce(sum(job.fetchedCount), 0)
            from NewsIngestionJob job
            where job.startedAt >= :start and job.startedAt < :end
            """)
    Long sumFetchedCountByStartedAtBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
