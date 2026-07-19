package com.pulsebrief.ingestion.repository;

import com.pulsebrief.ingestion.domain.NewsIngestionSource;
import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NewsIngestionSourceRepository extends JpaRepository<NewsIngestionSource, Long> {
    Optional<NewsIngestionSource> findByCode(String code);

    List<NewsIngestionSource> findAllByOrderByCodeAsc();

    List<NewsIngestionSource> findTop20ByScheduleEnabledAndEnabledAndNextRunAtLessThanEqualOrderByNextRunAtAsc(
            Byte scheduleEnabled,
            Byte enabled,
            LocalDateTime now
    );
}
