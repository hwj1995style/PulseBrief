package com.pulsebrief.ingestion.repository;

import com.pulsebrief.ingestion.domain.RawNewsContent;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RawNewsContentRepository extends JpaRepository<RawNewsContent, Long> {
    Optional<RawNewsContent> findTopByRawNewsItem_IdOrderByFetchedAtDesc(Long rawNewsItemId);

    Optional<RawNewsContent> findTopByRawNewsItem_IdAndFetchStatusOrderByFetchedAtDesc(Long rawNewsItemId, String fetchStatus);

    Optional<RawNewsContent> findByRawNewsItem_IdAndCaptureMode(Long rawNewsItemId, String captureMode);

    long countByContentTextHash(String contentTextHash);
}
