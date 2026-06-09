package com.pulsebrief.ingestion.repository;

import com.pulsebrief.ingestion.domain.RawNewsItem;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RawNewsItemRepository extends JpaRepository<RawNewsItem, Long> {
    boolean existsByOriginalUrlHash(String originalUrlHash);

    boolean existsBySourceCodeAndContentHash(String sourceCode, String contentHash);

    long countByOriginalUrl(String originalUrl);

    List<RawNewsItem> findByItemStatusOrderByFetchedAtAsc(String itemStatus, Pageable pageable);

    List<RawNewsItem> findBySourceCodeAndItemStatusOrderByFetchedAtAsc(
            String sourceCode,
            String itemStatus,
            Pageable pageable
    );
}
