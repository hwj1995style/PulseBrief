package com.pulsebrief.admin.service;

import com.pulsebrief.admin.api.AdminCandidateDetailResponse;
import com.pulsebrief.admin.api.AdminCandidatePublishRequest;
import com.pulsebrief.admin.api.AdminCandidateRejectRequest;
import com.pulsebrief.admin.api.AdminCandidateResponse;
import com.pulsebrief.admin.api.AdminCandidateUpdateRequest;
import com.pulsebrief.article.domain.NewsArticle;
import com.pulsebrief.article.repository.ArticleRepository;
import com.pulsebrief.common.api.PageResponse;
import com.pulsebrief.ingestion.domain.CandidateArticle;
import com.pulsebrief.ingestion.domain.ReportAsset;
import com.pulsebrief.ingestion.repository.CandidateArticleRepository;
import com.pulsebrief.ingestion.repository.ReportAssetRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
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

    private final CandidateArticleRepository candidateArticleRepository;
    private final ReportAssetRepository reportAssetRepository;
    private final ArticleRepository articleRepository;
    private final AdminCandidateMapper mapper;

    public AdminCandidateApplicationService(
            CandidateArticleRepository candidateArticleRepository,
            ReportAssetRepository reportAssetRepository,
            ArticleRepository articleRepository,
            AdminCandidateMapper mapper
    ) {
        this.candidateArticleRepository = candidateArticleRepository;
        this.reportAssetRepository = reportAssetRepository;
        this.articleRepository = articleRepository;
        this.mapper = mapper;
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
                List.of(),
                availableActions(candidate)
        );
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
        candidate.updateDraft(title, summary, blankToDefault(request.categoryCode(), candidate.getCategoryCode()));
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
                articleHash(candidate)
        ));
        candidate.publish(article.getId());
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

    private void assertReportAssetsApproved(Long candidateId) {
        boolean hasUnapprovedAsset = reportAssetRepository.findByCandidateArticle_IdOrderByCreatedAtDesc(candidateId)
                .stream()
                .anyMatch(asset -> !"APPROVED".equals(asset.getAssetStatus()));
        if (hasUnapprovedAsset) {
            throw new ResponseStatusException(CONFLICT, "Report assets require compliance approval before publish");
        }
    }

    private List<String> availableActions(CandidateArticle candidate) {
        if (!PENDING_REVIEW.equals(candidate.getStatus())) {
            return List.of();
        }
        return List.of("EDIT", "REJECT", "PUBLISH");
    }

    private String blankToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
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
