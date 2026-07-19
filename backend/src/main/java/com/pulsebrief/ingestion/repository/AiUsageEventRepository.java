package com.pulsebrief.ingestion.repository;

import com.pulsebrief.ingestion.domain.AiUsageEvent;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiUsageEventRepository extends JpaRepository<AiUsageEvent, Long> {
    List<AiUsageEvent> findByCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDesc(
            LocalDateTime start,
            LocalDateTime end
    );
}
