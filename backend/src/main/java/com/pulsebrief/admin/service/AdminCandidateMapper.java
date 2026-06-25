package com.pulsebrief.admin.service;

import com.pulsebrief.admin.api.AdminAiSummaryTaskResponse;
import com.pulsebrief.admin.api.AdminCandidateResponse;
import com.pulsebrief.admin.api.AdminRawNewsItemResponse;
import com.pulsebrief.admin.api.AdminReportAssetResponse;
import com.pulsebrief.ingestion.domain.AiSummaryTask;
import com.pulsebrief.ingestion.domain.CandidateArticle;
import com.pulsebrief.ingestion.domain.RawNewsItem;
import com.pulsebrief.ingestion.domain.ReportAsset;
import com.pulsebrief.ingestion.domain.ReportAssetFile;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
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
                candidate.getReviewNote(),
                parseTags(candidate.getTagNames())
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
        ReportAssetFile assetFile = reportAsset.getAssetFile();
        return new AdminReportAssetResponse(
                reportAsset.getId(),
                reportAsset.getTitle(),
                reportAsset.getOriginalUrl(),
                reportAsset.getFileName(),
                assetFile == null ? reportAsset.getFileSizeBytes() : assetFile.getFileSizeBytes(),
                assetFile == null ? reportAsset.getFileHash() : assetFile.getFileHash(),
                reportAsset.getLicensePolicy(),
                reportAsset.getAssetStatus(),
                reportAsset.getLicenseNote(),
                reportAsset.getCacheStatus(),
                reportAsset.getCacheErrorMessage(),
                assetFile == null ? null : assetFile.getMimeType(),
                formatTime(reportAsset.getCacheCompletedAt()),
                reportAsset.getReviewNote(),
                formatTime(reportAsset.getReviewedAt()),
                reportAsset.getReviewedBy()
        );
    }

    public AdminAiSummaryTaskResponse toAiSummaryTaskResponse(AiSummaryTask task) {
        if (task == null) {
            return null;
        }
        return new AdminAiSummaryTaskResponse(
                task.getId(),
                task.getTaskStatus(),
                task.getInputSourceType(),
                task.getInputRefId(),
                task.getInputPreview(),
                task.getProviderType(),
                task.getModelName(),
                task.getPromptVersion(),
                task.getGeneratedSummary(),
                parseLines(task.getGeneratedKeyPoints()),
                task.getGeneratedImpactAnalysis(),
                task.getErrorMessage(),
                task.getRequestedBy(),
                formatTime(task.getStartedAt()),
                formatTime(task.getFinishedAt())
        );
    }

    private String formatTime(LocalDateTime value) {
        return value == null ? null : value.atOffset(ZoneOffset.ofHours(8)).format(API_TIME);
    }

    private List<String> parseTags(String tagNames) {
        if (tagNames == null || tagNames.isBlank()) {
            return List.of();
        }
        return Arrays.stream(tagNames.split(","))
                .map(String::trim)
                .filter(tag -> !tag.isEmpty())
                .toList();
    }

    private List<String> parseLines(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split("\\R"))
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .toList();
    }
}
