package com.pulsebrief.ingestion.repository;

import com.pulsebrief.ingestion.domain.NewsIngestionSource;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NewsIngestionSourceRepository extends JpaRepository<NewsIngestionSource, Long> {
    Optional<NewsIngestionSource> findByCode(String code);

    List<NewsIngestionSource> findAllByOrderByCodeAsc();
}
