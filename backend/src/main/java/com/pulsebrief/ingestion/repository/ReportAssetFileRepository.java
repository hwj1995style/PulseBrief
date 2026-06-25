package com.pulsebrief.ingestion.repository;

import com.pulsebrief.ingestion.domain.ReportAssetFile;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportAssetFileRepository extends JpaRepository<ReportAssetFile, Long> {
    Optional<ReportAssetFile> findByFileHash(String fileHash);
}
