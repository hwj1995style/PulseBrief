package com.pulsebrief.ingestion.repository;

import com.pulsebrief.ingestion.domain.NewsIngestionSource;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NewsIngestionSourceRepository extends JpaRepository<NewsIngestionSource, Long> {
}
