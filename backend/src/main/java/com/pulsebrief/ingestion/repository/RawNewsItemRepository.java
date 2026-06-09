package com.pulsebrief.ingestion.repository;

import com.pulsebrief.ingestion.domain.RawNewsItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RawNewsItemRepository extends JpaRepository<RawNewsItem, Long> {
    boolean existsByOriginalUrlHash(String originalUrlHash);

    boolean existsBySourceCodeAndContentHash(String sourceCode, String contentHash);

    long countByOriginalUrl(String originalUrl);
}
