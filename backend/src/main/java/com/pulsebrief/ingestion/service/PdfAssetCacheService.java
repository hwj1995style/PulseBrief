package com.pulsebrief.ingestion.service;

import com.pulsebrief.ingestion.config.PdfCacheProperties;
import com.pulsebrief.ingestion.domain.CandidateArticle;
import com.pulsebrief.ingestion.domain.NewsIngestionSource;
import com.pulsebrief.ingestion.domain.ReportAsset;
import com.pulsebrief.ingestion.domain.ReportAssetFile;
import com.pulsebrief.ingestion.repository.CandidateArticleRepository;
import com.pulsebrief.ingestion.repository.NewsIngestionSourceRepository;
import com.pulsebrief.ingestion.repository.ReportAssetFileRepository;
import com.pulsebrief.ingestion.repository.ReportAssetRepository;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class PdfAssetCacheService {
    private static final DateTimeFormatter PATH_DATE = DateTimeFormatter.BASIC_ISO_DATE;

    private final CandidateArticleRepository candidateArticleRepository;
    private final ReportAssetRepository reportAssetRepository;
    private final ReportAssetFileRepository reportAssetFileRepository;
    private final NewsIngestionSourceRepository sourceRepository;
    private final PdfDownloadClient pdfDownloadClient;
    private final PdfCacheProperties properties;

    public PdfAssetCacheService(
            CandidateArticleRepository candidateArticleRepository,
            ReportAssetRepository reportAssetRepository,
            ReportAssetFileRepository reportAssetFileRepository,
            NewsIngestionSourceRepository sourceRepository,
            PdfDownloadClient pdfDownloadClient,
            PdfCacheProperties properties
    ) {
        this.candidateArticleRepository = candidateArticleRepository;
        this.reportAssetRepository = reportAssetRepository;
        this.reportAssetFileRepository = reportAssetFileRepository;
        this.sourceRepository = sourceRepository;
        this.pdfDownloadClient = pdfDownloadClient;
        this.properties = properties;
    }

    @Transactional
    public ReportAsset cacheAsset(Long candidateId, Long assetId) {
        CandidateArticle candidate = candidateArticleRepository.findById(candidateId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Candidate not found"));
        ReportAsset asset = reportAssetRepository.findById(assetId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Report asset not found"));
        if (!candidate.getId().equals(asset.getCandidateArticle().getId())) {
            throw new ResponseStatusException(NOT_FOUND, "Report asset not found for candidate");
        }

        NewsIngestionSource source = sourceRepository.findByCode(asset.getSourceCode()).orElse(null);
        asset.markCachePending();

        String skipReason = skipReason(candidate, asset, source);
        if (skipReason != null) {
            asset.markCacheSkipped(skipReason, source == null ? null : source.getLicenseNote());
            return asset;
        }

        try {
            DownloadedPdf downloadedPdf = pdfDownloadClient.download(asset.getOriginalUrl());
            validateDownloadedPdf(downloadedPdf);
            String fileHash = sha256(downloadedPdf.bytes());
            ReportAssetFile assetFile = reportAssetFileRepository.findByFileHash(fileHash)
                    .orElseGet(() -> storePdfFile(asset, downloadedPdf, fileHash));
            asset.markCacheSuccess(assetFile, source.getLicenseNote());
            return asset;
        } catch (RuntimeException exception) {
            asset.markCacheFailed(errorMessage(exception), source == null ? null : source.getLicenseNote());
            return asset;
        }
    }

    private String skipReason(CandidateArticle candidate, ReportAsset asset, NewsIngestionSource source) {
        if (!properties.isEnabled()) {
            return "PDF cache is disabled";
        }
        if (source == null) {
            return "PDF cache source is not configured";
        }
        if (!source.isEnabled()) {
            return "PDF cache source is disabled";
        }
        if (!"PDF_ALLOWED".equals(source.getContentAccessPolicy())
                || !source.isPdfDownloadAllowed()
                || !"PDF_ALLOWED".equals(asset.getLicensePolicy())) {
            return "PDF cache is not authorized for this source";
        }
        if (source.getLicenseNote() == null || source.getLicenseNote().isBlank()) {
            return "PDF cache requires license note";
        }
        if (!isPublicHttpUrl(asset.getOriginalUrl())) {
            return "PDF URL must be public HTTP(S)";
        }
        Integer maxAgeHours = source.getMaxAgeHours();
        if (maxAgeHours != null
                && maxAgeHours > 0
                && candidate.getRawNewsItem().getPublishedAt() != null
                && candidate.getRawNewsItem().getPublishedAt().isBefore(LocalDateTime.now().minusHours(maxAgeHours))) {
            return "Candidate is outside latest PDF cache window";
        }
        return null;
    }

    private boolean isPublicHttpUrl(String value) {
        return value != null
                && (value.startsWith("https://") || value.startsWith("http://"));
    }

    private void validateDownloadedPdf(DownloadedPdf downloadedPdf) {
        if (downloadedPdf == null || downloadedPdf.bytes() == null || downloadedPdf.bytes().length == 0) {
            throw new IllegalStateException("Downloaded PDF is empty");
        }
        if (downloadedPdf.bytes().length > properties.maxSizeBytes()) {
            throw new IllegalStateException("PDF file exceeds configured size limit");
        }
        String mimeType = downloadedPdf.mimeType();
        if (mimeType == null || (!"application/pdf".equalsIgnoreCase(mimeType) && !mimeType.toLowerCase().contains("pdf"))) {
            throw new IllegalStateException("Downloaded file is not a PDF");
        }
    }

    private ReportAssetFile storePdfFile(ReportAsset asset, DownloadedPdf downloadedPdf, String fileHash) {
        try {
            String sourceSegment = sanitizePathSegment(asset.getSourceCode());
            String dateSegment = LocalDate.now().format(PATH_DATE);
            String fileName = fileHash + ".pdf";
            Path baseDirectory = Paths.get(properties.getStorageDir()).toAbsolutePath().normalize();
            Path relativePath = Paths.get(sourceSegment, dateSegment, fileName);
            Path targetPath = baseDirectory.resolve(relativePath).normalize();
            Files.createDirectories(targetPath.getParent());
            Files.write(targetPath, downloadedPdf.bytes());
            return reportAssetFileRepository.save(new ReportAssetFile(
                    fileHash,
                    "LOCAL",
                    relativePath.toString().replace('\\', '/'),
                    safeFileName(downloadedPdf.fileName()),
                    (long) downloadedPdf.bytes().length,
                    downloadedPdf.mimeType()
            ));
        } catch (IOException exception) {
            throw new IllegalStateException("PDF cache file write failed: " + exception.getMessage(), exception);
        }
    }

    private String sanitizePathSegment(String value) {
        if (value == null || value.isBlank()) {
            return "unknown-source";
        }
        String sanitized = value.replaceAll("[^A-Za-z0-9._-]", "-");
        return sanitized.isBlank() ? "unknown-source" : sanitized;
    }

    private String safeFileName(String value) {
        if (value == null || value.isBlank()) {
            return "report.pdf";
        }
        String sanitized = value.replaceAll("[\\\\/]", "-").trim();
        return sanitized.isBlank() ? "report.pdf" : sanitized;
    }

    private String sha256(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(bytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private String errorMessage(RuntimeException exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            message = exception.getClass().getSimpleName();
        }
        byte[] bytes = message.getBytes(StandardCharsets.UTF_8);
        if (bytes.length <= 1000) {
            return message;
        }
        return new String(bytes, 0, 1000, StandardCharsets.UTF_8);
    }
}
