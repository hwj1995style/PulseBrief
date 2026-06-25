package com.pulsebrief.ingestion.repository;

import com.pulsebrief.ingestion.domain.AiSummaryTask;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiSummaryTaskRepository extends JpaRepository<AiSummaryTask, Long> {
    Optional<AiSummaryTask> findTopByCandidateArticle_IdOrderByCreatedAtDesc(Long candidateArticleId);

    Optional<AiSummaryTask> findByIdAndCandidateArticle_Id(Long id, Long candidateArticleId);
}
