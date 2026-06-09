package com.pulsebrief.ingestion.repository;

import com.pulsebrief.ingestion.domain.ReportAsset;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportAssetRepository extends JpaRepository<ReportAsset, Long> {
    Optional<ReportAsset> findByFileHash(String fileHash);

    long countByFileHash(String fileHash);

    List<ReportAsset> findByCandidateArticle_IdOrderByCreatedAtDesc(Long candidateArticleId);
}
