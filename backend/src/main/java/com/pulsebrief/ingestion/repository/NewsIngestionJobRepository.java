package com.pulsebrief.ingestion.repository;

import com.pulsebrief.ingestion.domain.NewsIngestionJob;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NewsIngestionJobRepository extends JpaRepository<NewsIngestionJob, Long> {
}
