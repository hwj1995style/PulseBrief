package com.pulsebrief.ingestion.service;

import com.pulsebrief.ingestion.domain.NewsIngestionSource;
import com.pulsebrief.ingestion.domain.RawNewsContent;
import com.pulsebrief.ingestion.domain.RawNewsItem;
import com.pulsebrief.ingestion.repository.NewsIngestionSourceRepository;
import com.pulsebrief.ingestion.repository.RawNewsContentRepository;
import com.pulsebrief.ingestion.repository.RawNewsItemRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class ContentFetchService {
    private static final int SNIPPET_MAX_LENGTH = 1_000;
    private static final int FULLTEXT_MAX_LENGTH = 20_000;

    private final RawNewsItemRepository rawNewsItemRepository;
    private final NewsIngestionSourceRepository sourceRepository;
    private final RawNewsContentRepository contentRepository;
    private final HtmlContentClient htmlContentClient;
    private final HtmlContentExtractor htmlContentExtractor;

    public ContentFetchService(
            RawNewsItemRepository rawNewsItemRepository,
            NewsIngestionSourceRepository sourceRepository,
            RawNewsContentRepository contentRepository,
            HtmlContentClient htmlContentClient,
            HtmlContentExtractor htmlContentExtractor
    ) {
        this.rawNewsItemRepository = rawNewsItemRepository;
        this.sourceRepository = sourceRepository;
        this.contentRepository = contentRepository;
        this.htmlContentClient = htmlContentClient;
        this.htmlContentExtractor = htmlContentExtractor;
    }

    @Transactional
    public ContentFetchResult fetchRawItem(Long rawNewsItemId, ContentFetchMode mode) {
        RawNewsItem rawItem = rawNewsItemRepository.findById(rawNewsItemId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Raw news item not found"));
        NewsIngestionSource source = sourceRepository.findByCode(rawItem.getSourceCode()).orElse(null);
        ContentFetchMode safeMode = mode == null ? ContentFetchMode.SNIPPET : mode;

        String skipReason = skipReason(rawItem, source, safeMode);
        if (skipReason != null) {
            return skipped(rawItem, safeMode, source, skipReason);
        }

        return contentRepository.findByRawNewsItem_IdAndCaptureMode(rawItem.getId(), safeMode.name())
                .map(this::toResult)
                .orElseGet(() -> fetchAndSave(rawItem, source, safeMode));
    }

    private ContentFetchResult fetchAndSave(RawNewsItem rawItem, NewsIngestionSource source, ContentFetchMode mode) {
        try {
            String html = htmlContentClient.fetch(rawItem.getOriginalUrl());
            String text = htmlContentExtractor.extract(html);
            if (text.isBlank()) {
                return saveFailure(rawItem, mode, source, "Content extraction produced empty text");
            }
            String capturedText = trimToMode(text, mode);
            RawNewsContent content = RawNewsContent.success(
                    rawItem,
                    mode.name(),
                    source.getContentAccessPolicy(),
                    source.getLicenseNote(),
                    capturedText,
                    sha256(capturedText)
            );
            return toResult(contentRepository.save(content));
        } catch (RuntimeException exception) {
            String message = exception.getMessage() == null
                    ? exception.getClass().getSimpleName()
                    : exception.getMessage();
            return saveFailure(rawItem, mode, source, message);
        }
    }

    private String skipReason(RawNewsItem rawItem, NewsIngestionSource source, ContentFetchMode mode) {
        if (source == null) {
            return "Content fetch source is not configured";
        }
        if (!source.isEnabled()) {
            return "Content fetch source is disabled";
        }
        String licenseNote = source.getLicenseNote();
        if (licenseNote == null || licenseNote.isBlank()) {
            return "Content fetch requires license note";
        }
        String policy = source.getContentAccessPolicy();
        if (!"SNIPPET_ALLOWED".equals(policy) && !"FULLTEXT_ALLOWED".equals(policy)) {
            return "Content fetch is not authorized for policy " + policy;
        }
        if (mode == ContentFetchMode.FULLTEXT && !"FULLTEXT_ALLOWED".equals(policy)) {
            return "Full text fetch is not authorized for policy " + policy;
        }
        Integer maxAgeHours = source.getMaxAgeHours();
        if (maxAgeHours != null
                && maxAgeHours > 0
                && rawItem.getPublishedAt() != null
                && rawItem.getPublishedAt().isBefore(LocalDateTime.now().minusHours(maxAgeHours))) {
            return "Raw item is outside latest window";
        }
        return null;
    }

    private ContentFetchResult skipped(
            RawNewsItem rawItem,
            ContentFetchMode mode,
            NewsIngestionSource source,
            String message
    ) {
        return new ContentFetchResult(
                rawItem.getId(),
                mode.name(),
                "SKIPPED",
                null,
                source == null ? null : source.getContentAccessPolicy(),
                source == null ? null : source.getLicenseNote(),
                LocalDateTime.now(),
                message
        );
    }

    private ContentFetchResult saveFailure(
            RawNewsItem rawItem,
            ContentFetchMode mode,
            NewsIngestionSource source,
            String message
    ) {
        RawNewsContent content = RawNewsContent.failed(
                rawItem,
                mode.name(),
                source.getContentAccessPolicy(),
                source.getLicenseNote(),
                message
        );
        return toResult(contentRepository.save(content));
    }

    private String trimToMode(String text, ContentFetchMode mode) {
        int maxLength = mode == ContentFetchMode.FULLTEXT ? FULLTEXT_MAX_LENGTH : SNIPPET_MAX_LENGTH;
        return text.length() <= maxLength ? text : text.substring(0, maxLength).trim();
    }

    private ContentFetchResult toResult(RawNewsContent content) {
        return new ContentFetchResult(
                content.getRawNewsItem().getId(),
                content.getCaptureMode(),
                content.getFetchStatus(),
                preview(content.getContentText()),
                content.getLicensePolicy(),
                content.getLicenseNote(),
                content.getFetchedAt(),
                content.getErrorMessage()
        );
    }

    private String preview(String contentText) {
        if (contentText == null || contentText.isBlank()) {
            return null;
        }
        return contentText.length() <= 500 ? contentText : contentText.substring(0, 500).trim();
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
