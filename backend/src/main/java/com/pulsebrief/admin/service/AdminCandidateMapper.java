package com.pulsebrief.admin.service;

import com.pulsebrief.admin.api.AdminCandidateResponse;
import com.pulsebrief.admin.api.AdminRawNewsItemResponse;
import com.pulsebrief.admin.api.AdminReportAssetResponse;
import com.pulsebrief.ingestion.domain.CandidateArticle;
import com.pulsebrief.ingestion.domain.RawNewsItem;
import com.pulsebrief.ingestion.domain.ReportAsset;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;

@Component
public class AdminCandidateMapper {
    private static final DateTimeFormatter API_TIME = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    public AdminCandidateResponse toCandidateResponse(CandidateArticle candidate) {
        return new AdminCandidateResponse(
                candidate.getId(),
                candidate.getRawNewsItem().getId(),
                candidate.getTitle(),
                candidate.getSummary(),
                candidate.getCategoryCode(),
                candidate.getSourceName(),
                candidate.getOriginalUrl(),
                formatTime(candidate.getPublishedAt()),
                candidate.getStatus(),
                formatTime(candidate.getCreatedAt()),
                candidate.getPublishedArticleId(),
                candidate.getReviewNote()
        );
    }

    public AdminRawNewsItemResponse toRawItemResponse(RawNewsItem rawItem) {
        return new AdminRawNewsItemResponse(
                rawItem.getId(),
                rawItem.getSourceCode(),
                rawItem.getProviderItemId(),
                rawItem.getTitle(),
                rawItem.getSummary(),
                rawItem.getSourceName(),
                rawItem.getOriginalUrl(),
                formatTime(rawItem.getPublishedAt()),
                formatTime(rawItem.getFetchedAt()),
                rawItem.getLanguage(),
                rawItem.getCountry(),
                rawItem.getItemStatus()
        );
    }

    public AdminReportAssetResponse toReportAssetResponse(ReportAsset reportAsset) {
        return new AdminReportAssetResponse(
                reportAsset.getId(),
                reportAsset.getTitle(),
                reportAsset.getOriginalUrl(),
                reportAsset.getFileName(),
                reportAsset.getFileSizeBytes(),
                reportAsset.getFileHash(),
                reportAsset.getLicensePolicy(),
                reportAsset.getAssetStatus()
        );
    }

    private String formatTime(LocalDateTime value) {
        return value == null ? null : value.atOffset(ZoneOffset.ofHours(8)).format(API_TIME);
    }
}
