package com.pulsebrief.admin.service;

import com.pulsebrief.admin.api.AdminCandidateDetailResponse;
import com.pulsebrief.admin.api.AdminAiSummaryGenerateRequest;
import com.pulsebrief.admin.api.AdminAiSummaryTaskResponse;
import com.pulsebrief.admin.api.AdminCandidateContentFetchRequest;
import com.pulsebrief.admin.api.AdminCandidateContentResponse;
import com.pulsebrief.admin.api.AdminCandidatePublishRequest;
import com.pulsebrief.admin.api.AdminCandidateRejectRequest;
import com.pulsebrief.admin.api.AdminCandidateResponse;
import com.pulsebrief.admin.api.AdminCandidateUpdateRequest;
import com.pulsebrief.admin.api.AdminReportAssetActionRequest;
import com.pulsebrief.admin.api.AdminReportAssetResponse;
import com.pulsebrief.article.domain.NewsArticle;
import com.pulsebrief.article.repository.ArticleRepository;
import com.pulsebrief.common.api.PageResponse;
import com.pulsebrief.ingestion.domain.CandidateArticle;
import com.pulsebrief.ingestion.domain.AiSummaryTask;
import com.pulsebrief.ingestion.domain.RawNewsContent;
import com.pulsebrief.ingestion.domain.ReportAsset;
import com.pulsebrief.ingestion.repository.CandidateArticleRepository;
import com.pulsebrief.ingestion.repository.RawNewsContentRepository;
import com.pulsebrief.ingestion.repository.ReportAssetRepository;
import com.pulsebrief.ingestion.service.ContentFetchMode;
import com.pulsebrief.ingestion.service.ContentFetchResult;
import com.pulsebrief.ingestion.service.ContentFetchService;
import com.pulsebrief.ingestion.service.PdfAssetCacheService;
import com.pulsebrief.ingestion.service.AiSummaryTaskService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;

@Service
public class AdminCandidateApplicationService {
    private static final String PENDING_REVIEW = "PENDING_REVIEW";
    private static final int MAX_TAG_COUNT = 8;
    private static final int MAX_TAG_LENGTH = 24;

    private final CandidateArticleRepository candidateArticleRepository;
    private final ReportAssetRepository reportAssetRepository;
    private final RawNewsContentRepository rawNewsContentRepository;
    private final ArticleRepository articleRepository;
    private final AdminCandidateMapper mapper;
    private final AdminOperationLogService operationLogService;
    private final ContentFetchService contentFetchService;
    private final PdfAssetCacheService pdfAssetCacheService;
    private final AiSummaryTaskService aiSummaryTaskService;

    public AdminCandidateApplicationService(
            CandidateArticleRepository candidateArticleRepository,
            ReportAssetRepository reportAssetRepository,
            RawNewsContentRepository rawNewsContentRepository,
            ArticleRepository articleRepository,
            AdminCandidateMapper mapper,
            AdminOperationLogService operationLogService,
            ContentFetchService contentFetchService,
            PdfAssetCacheService pdfAssetCacheService,
            AiSummaryTaskService aiSummaryTaskService
    ) {
        this.candidateArticleRepository = candidateArticleRepository;
        this.reportAssetRepository = reportAssetRepository;
        this.rawNewsContentRepository = rawNewsContentRepository;
        this.articleRepository = articleRepository;
        this.mapper = mapper;
        this.operationLogService = operationLogService;
        this.contentFetchService = contentFetchService;
        this.pdfAssetCacheService = pdfAssetCacheService;
        this.aiSummaryTaskService = aiSummaryTaskService;
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminCandidateResponse> listCandidates(String status, Integer page, Integer pageSize) {
        String safeStatus = status == null || status.isBlank() ? PENDING_REVIEW : status;
        int safePage = Math.max(page == null ? 1 : page, 1);
        int safePageSize = Math.min(Math.max(pageSize == null ? 20 : pageSize, 1), 50);
        Page<CandidateArticle> candidates = candidateArticleRepository.findByStatusOrderByCreatedAtDesc(
                safeStatus,
                PageRequest.of(safePage - 1, safePageSize)
        );
        return PageResponse.of(
                candidates.getContent().stream().map(mapper::toCandidateResponse).toList(),
                safePage,
                safePageSize,
                candidates.getTotalElements()
        );
    }

    @Transactional(readOnly = true)
    public AdminCandidateDetailResponse getCandidate(Long id) {
        CandidateArticle candidate = requireCandidate(id);
        List<ReportAsset> reportAssets = reportAssetRepository.findByCandidateArticle_IdOrderByCreatedAtDesc(id);
        return new AdminCandidateDetailResponse(
                mapper.toCandidateResponse(candidate),
                mapper.toRawItemResponse(candidate.getRawNewsItem()),
                reportAssets.stream().map(mapper::toReportAssetResponse).toList(),
                rawNewsContentRepository
                        .findTopByRawNewsItem_IdOrderByFetchedAtDesc(candidate.getRawNewsItem().getId())
                        .map(content -> toContentResponse(candidate.getId(), content))
                        .orElse(null),
                mapper.toAiSummaryTaskResponse(aiSummaryTaskService.latestTask(id)),
                List.of(),
                availableActions(candidate)
        );
    }

    @Transactional
    public AdminCandidateContentResponse fetchCandidateContent(
            Long id,
            AdminCandidateContentFetchRequest request
    ) {
        CandidateArticle candidate = requirePendingCandidate(id);
        ContentFetchMode mode = ContentFetchMode.valueOf(request.modeOrDefault());
        ContentFetchResult result = contentFetchService.fetchRawItem(candidate.getRawNewsItem().getId(), mode);
        return toContentResponse(candidate.getId(), result);
    }

    @Transactional
    public AdminAiSummaryTaskResponse generateAiSummary(
            Long id,
            AdminAiSummaryGenerateRequest request
    ) {
        AiSummaryTask task = aiSummaryTaskService.generate(
                id,
                request.inputSourceType(),
                request.providerType(),
                request.promptVersion()
        );
        return mapper.toAiSummaryTaskResponse(task);
    }

    @Transactional(readOnly = true)
    public AdminAiSummaryTaskResponse applyAiSummary(Long id, Long taskId) {
        return mapper.toAiSummaryTaskResponse(aiSummaryTaskService.apply(id, taskId));
    }

    @Transactional
    public AdminReportAssetResponse cacheReportAsset(Long candidateId, Long assetId) {
        requirePendingCandidate(candidateId);
        requireReportAsset(candidateId, assetId);
        ReportAsset asset = pdfAssetCacheService.cacheAsset(candidateId, assetId);
        return mapper.toReportAssetResponse(asset);
    }

    @Transactional
    public AdminReportAssetResponse approveReportAsset(
            Long candidateId,
            Long assetId,
            AdminReportAssetActionRequest request
    ) {
        requirePendingCandidate(candidateId);
        ReportAsset asset = requireReportAsset(candidateId, assetId);
        if (!"SUCCESS".equals(asset.getCacheStatus())) {
            throw new ResponseStatusException(CONFLICT, "Report asset requires successful PDF cache before approval");
        }
        asset.approve(request == null ? null : request.reviewNote(), "dev-admin");
        return mapper.toReportAssetResponse(asset);
    }

    @Transactional
    public AdminReportAssetResponse rejectReportAsset(
            Long candidateId,
            Long assetId,
            AdminReportAssetActionRequest request
    ) {
        requirePendingCandidate(candidateId);
        ReportAsset asset = requireReportAsset(candidateId, assetId);
        asset.reject(request == null ? null : request.reviewNote(), "dev-admin");
        return mapper.toReportAssetResponse(asset);
    }

    @Transactional
    public AdminCandidateResponse updateCandidate(Long id, AdminCandidateUpdateRequest request) {
        CandidateArticle candidate = requirePendingCandidate(id);
        if (request == null) {
            throw new ResponseStatusException(UNPROCESSABLE_ENTITY, "Candidate update request is required");
        }
        String title = request.title();
        if (title == null || title.isBlank()) {
            throw new ResponseStatusException(UNPROCESSABLE_ENTITY, "Candidate title is required");
        }
        String summary = request.summary();
        if (summary != null && summary.length() > 2000) {
            throw new ResponseStatusException(UNPROCESSABLE_ENTITY, "Candidate summary is too long");
        }
        String categoryCode = blankToDefault(request.categoryCode(), candidate.getCategoryCode());
        if (!categoryCode.equals(candidate.getSuggestedCategoryCode())
                && (request.categoryOverrideReason() == null || request.categoryOverrideReason().isBlank())) {
            throw new ResponseStatusException(
                    UNPROCESSABLE_ENTITY,
                    "Category override reason is required when final category differs from suggestion"
            );
        }
        candidate.updateDraft(
                title,
                summary,
                categoryCode,
                blankToDefault(request.sourceName(), candidate.getSourceName()),
                normalizeTags(request.tagNames()),
                request.categoryOverrideReason()
        );
        return mapper.toCandidateResponse(candidate);
    }

    @Transactional
    public AdminCandidateResponse rejectCandidate(Long id, AdminCandidateRejectRequest request) {
        CandidateArticle candidate = requirePendingCandidate(id);
        candidate.reject(request == null ? null : request.reviewNote());
        return mapper.toCandidateResponse(candidate);
    }

    @Transactional
    public AdminCandidateResponse publishCandidate(Long id, AdminCandidatePublishRequest request) {
        CandidateArticle candidate = requirePendingCandidate(id);
        assertReportAssetsApproved(id);

        String title = blankToDefault(request == null ? null : request.title(), candidate.getTitle());
        String summary = blankToDefault(request == null ? null : request.summary(), candidate.getSummary());
        String categoryCode = blankToDefault(request == null ? null : request.categoryCode(), candidate.getCategoryCode());
        String categoryOverrideReason = blankToDefault(
                request == null ? null : request.categoryOverrideReason(),
                candidate.getCategoryOverrideReason()
        );
        if (!categoryCode.equals(candidate.getSuggestedCategoryCode())
                && (categoryOverrideReason == null || categoryOverrideReason.isBlank())) {
            throw new ResponseStatusException(
                    UNPROCESSABLE_ENTITY,
                    "Category override reason is required when final category differs from suggestion"
            );
        }
        candidate.confirmCategory(categoryCode, categoryOverrideReason);
        String aiSummary = blankToDefault(request == null ? null : request.aiSummary(), summary);
        String impactAnalysis = request == null ? null : request.impactAnalysis();
        String keyPoints = request == null || request.keyPoints() == null
                ? ""
                : String.join(System.lineSeparator(), request.keyPoints());

        NewsArticle article = articleRepository.save(new NewsArticle(
                title,
                summary,
                aiSummary,
                keyPoints,
                impactAnalysis,
                candidate.getSourceName(),
                candidate.getOriginalUrl(),
                categoryCode,
                LocalDateTime.now(),
                articleHash(candidate),
                candidate.getTagNames()
        ));
        candidate.publish(article.getId());
        operationLogService.recordArticlePublish(article.getId(), article.getTitle());
        return mapper.toCandidateResponse(candidate);
    }

    private CandidateArticle requireCandidate(Long id) {
        return candidateArticleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Candidate not found"));
    }

    private CandidateArticle requirePendingCandidate(Long id) {
        CandidateArticle candidate = requireCandidate(id);
        if (!PENDING_REVIEW.equals(candidate.getStatus())) {
            throw new ResponseStatusException(CONFLICT, "Candidate is not pending review");
        }
        return candidate;
    }

    private ReportAsset requireReportAsset(Long candidateId, Long assetId) {
        return reportAssetRepository.findByIdAndCandidateArticle_Id(assetId, candidateId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Report asset not found"));
    }

    private void assertReportAssetsApproved(Long candidateId) {
        boolean hasBlockingAsset = reportAssetRepository.findByCandidateArticle_IdOrderByCreatedAtDesc(candidateId)
                .stream()
                .anyMatch(asset -> !"APPROVED".equals(asset.getAssetStatus())
                        && !"REJECTED".equals(asset.getAssetStatus()));
        if (hasBlockingAsset) {
            throw new ResponseStatusException(CONFLICT, "Report assets require compliance approval before publish");
        }
    }

    private List<String> availableActions(CandidateArticle candidate) {
        if (!PENDING_REVIEW.equals(candidate.getStatus())) {
            return List.of();
        }
        return List.of("EDIT", "REJECT", "PUBLISH");
    }

    private AdminCandidateContentResponse toContentResponse(Long candidateId, RawNewsContent content) {
        return new AdminCandidateContentResponse(
                candidateId,
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

    private AdminCandidateContentResponse toContentResponse(Long candidateId, ContentFetchResult result) {
        return new AdminCandidateContentResponse(
                candidateId,
                result.rawNewsItemId(),
                result.captureMode(),
                result.fetchStatus(),
                result.preview(),
                result.licensePolicy(),
                result.licenseNote(),
                result.fetchedAt(),
                result.errorMessage()
        );
    }

    private String preview(String contentText) {
        if (contentText == null || contentText.isBlank()) {
            return null;
        }
        return contentText.length() <= 500 ? contentText : contentText.substring(0, 500).trim();
    }

    private String blankToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    private String normalizeTags(List<String> tagNames) {
        if (tagNames == null || tagNames.isEmpty()) {
            return null;
        }
        Set<String> normalizedTags = new LinkedHashSet<>();
        for (String tagName : tagNames) {
            if (tagName == null || tagName.isBlank()) {
                continue;
            }
            String tag = tagName.trim();
            if (tag.length() > MAX_TAG_LENGTH) {
                throw new ResponseStatusException(UNPROCESSABLE_ENTITY, "Candidate tag is too long");
            }
            normalizedTags.add(tag);
            if (normalizedTags.size() > MAX_TAG_COUNT) {
                throw new ResponseStatusException(UNPROCESSABLE_ENTITY, "Candidate tags are too many");
            }
        }
        return normalizedTags.isEmpty() ? null : String.join(",", normalizedTags);
    }

    private String articleHash(CandidateArticle candidate) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest((candidate.getId() + ":" + candidate.getOriginalUrl())
                    .getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
