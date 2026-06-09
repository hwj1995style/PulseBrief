package com.pulsebrief.ingestion.service;

import com.pulsebrief.ingestion.domain.CandidateArticle;
import com.pulsebrief.ingestion.domain.ReportAsset;
import com.pulsebrief.ingestion.repository.CandidateArticleRepository;
import com.pulsebrief.ingestion.repository.ReportAssetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class ReportAssetRegistrationService {
    private final CandidateArticleRepository candidateArticleRepository;
    private final ReportAssetRepository reportAssetRepository;

    public ReportAssetRegistrationService(
            CandidateArticleRepository candidateArticleRepository,
            ReportAssetRepository reportAssetRepository
    ) {
        this.candidateArticleRepository = candidateArticleRepository;
        this.reportAssetRepository = reportAssetRepository;
    }

    @Transactional
    public ReportAsset registerPdfMetadata(
            Long candidateArticleId,
            String sourceCode,
            String title,
            String originalUrl,
            String fileName,
            Long fileSizeBytes,
            String fileHash,
            String licensePolicy
    ) {
        validateMetadata(title, originalUrl, fileName, fileHash, licensePolicy);
        return reportAssetRepository.findByFileHash(fileHash)
                .orElseGet(() -> createAsset(
                        candidateArticleId,
                        sourceCode,
                        title,
                        originalUrl,
                        fileName,
                        fileSizeBytes,
                        fileHash,
                        licensePolicy
                ));
    }

    private ReportAsset createAsset(
            Long candidateArticleId,
            String sourceCode,
            String title,
            String originalUrl,
            String fileName,
            Long fileSizeBytes,
            String fileHash,
            String licensePolicy
    ) {
        CandidateArticle candidate = candidateArticleRepository.findById(candidateArticleId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Candidate article not found"));
        return reportAssetRepository.save(new ReportAsset(
                candidate,
                sourceCode,
                title.trim(),
                originalUrl.trim(),
                fileName.trim(),
                fileSizeBytes,
                fileHash.trim(),
                licensePolicy.trim()
        ));
    }

    private void validateMetadata(
            String title,
            String originalUrl,
            String fileName,
            String fileHash,
            String licensePolicy
    ) {
        if (isBlank(title) || isBlank(originalUrl) || isBlank(fileName) || isBlank(fileHash)) {
            throw new IllegalArgumentException("Report asset metadata is incomplete");
        }
        if (!"PDF_ALLOWED".equals(licensePolicy)) {
            throw new IllegalArgumentException("PDF metadata requires PDF_ALLOWED license policy");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
