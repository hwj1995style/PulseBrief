package com.pulsebrief.ingestion.repository;

import com.pulsebrief.ingestion.domain.CandidateArticle;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CandidateArticleRepository extends JpaRepository<CandidateArticle, Long> {
    boolean existsByRawNewsItem_Id(Long rawNewsItemId);

    Optional<CandidateArticle> findByTitle(String title);

    long countByOriginalUrl(String originalUrl);

    Page<CandidateArticle> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
}
