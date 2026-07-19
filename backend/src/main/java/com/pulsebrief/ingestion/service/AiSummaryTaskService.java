package com.pulsebrief.ingestion.service;

import com.pulsebrief.admin.security.AdminIdentityService;
import com.pulsebrief.ingestion.domain.AiSummaryTask;
import com.pulsebrief.ingestion.domain.CandidateArticle;
import com.pulsebrief.ingestion.domain.RawNewsContent;
import com.pulsebrief.ingestion.repository.AiSummaryTaskRepository;
import com.pulsebrief.ingestion.repository.CandidateArticleRepository;
import com.pulsebrief.ingestion.repository.RawNewsContentRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;

@Service
public class AiSummaryTaskService {
    private static final String PENDING_REVIEW = "PENDING_REVIEW";
    private static final String DEFAULT_PROVIDER = "MOCK";
    private static final String DEFAULT_PROMPT_VERSION = "candidate-summary-v1";

    private final CandidateArticleRepository candidateArticleRepository;
    private final RawNewsContentRepository rawNewsContentRepository;
    private final AiSummaryTaskRepository aiSummaryTaskRepository;
    private final Map<String, AiSummaryProvider> providers;
    private final AdminIdentityService identityService;

    public AiSummaryTaskService(
            CandidateArticleRepository candidateArticleRepository,
            RawNewsContentRepository rawNewsContentRepository,
            AiSummaryTaskRepository aiSummaryTaskRepository,
            List<AiSummaryProvider> providers,
            AdminIdentityService identityService
    ) {
        this.candidateArticleRepository = candidateArticleRepository;
        this.rawNewsContentRepository = rawNewsContentRepository;
        this.aiSummaryTaskRepository = aiSummaryTaskRepository;
        this.providers = providers.stream()
                .collect(Collectors.toUnmodifiableMap(
                        provider -> provider.providerType().toUpperCase(Locale.ROOT),
                        Function.identity()
                ));
        this.identityService = identityService;
    }

    @Transactional
    public AiSummaryTask generate(
            Long candidateId,
            String requestedInputSourceType,
            String requestedProviderType,
            String requestedPromptVersion
    ) {
        CandidateArticle candidate = requirePendingCandidate(candidateId);
        String providerType = blankToDefault(requestedProviderType, DEFAULT_PROVIDER).toUpperCase(Locale.ROOT);
        String promptVersion = blankToDefault(requestedPromptVersion, DEFAULT_PROMPT_VERSION);
        AiSummaryProvider provider = requireProvider(providerType);
        AiSummaryInput input = selectInput(candidate, requestedInputSourceType);
        AiSummaryTask task = aiSummaryTaskRepository.save(new AiSummaryTask(
                candidate,
                input.sourceType(),
                input.refId(),
                input.textHash(),
                input.preview(),
                provider.providerType(),
                provider.modelName(),
                promptVersion,
                identityService.current().username()
        ));
        if (input.text() == null || input.text().isBlank()) {
            task.markSkipped("AI summary input is empty");
            return task;
        }

        task.markRunning();
        try {
            AiSummaryProviderResult result = provider.generate(new AiSummaryRequest(
                    candidate.getTitle(),
                    candidate.getSourceName(),
                    candidate.getPublishedAt(),
                    input.sourceType(),
                    input.text(),
                    input.preview(),
                    promptVersion
            ));
            task.markSuccess(
                    result.summary(),
                    String.join(System.lineSeparator(), result.keyPoints()),
                    result.impactAnalysis(),
                    result.tokenPromptCount(),
                    result.tokenCompletionCount()
            );
        } catch (RuntimeException exception) {
            task.markFailed(errorMessage(exception));
        }
        return task;
    }

    @Transactional(readOnly = true)
    public AiSummaryTask latestTask(Long candidateId) {
        return aiSummaryTaskRepository.findTopByCandidateArticle_IdOrderByCreatedAtDesc(candidateId)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public AiSummaryTask apply(Long candidateId, Long taskId) {
        requirePendingCandidate(candidateId);
        AiSummaryTask task = aiSummaryTaskRepository.findByIdAndCandidateArticle_Id(taskId, candidateId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "AI summary task not found"));
        if (!"SUCCESS".equals(task.getTaskStatus())) {
            throw new ResponseStatusException(CONFLICT, "Only successful AI summary tasks can be applied");
        }
        return task;
    }

    private AiSummaryInput selectInput(CandidateArticle candidate, String requestedInputSourceType) {
        String safeRequestedType = requestedInputSourceType == null || requestedInputSourceType.isBlank()
                ? "AUTO"
                : requestedInputSourceType.trim().toUpperCase(Locale.ROOT);
        if ("RSS_SUMMARY".equals(safeRequestedType)) {
            return rssInput(candidate);
        }
        return rawNewsContentRepository
                .findTopByRawNewsItem_IdAndFetchStatusOrderByFetchedAtDesc(candidate.getRawNewsItem().getId(), "SUCCESS")
                .filter(this::isAuthorizedAiInput)
                .map(this::contentInput)
                .orElseGet(() -> rssInput(candidate));
    }

    private boolean isAuthorizedAiInput(RawNewsContent content) {
        if (content.getContentText() == null || content.getContentText().isBlank()) {
            return false;
        }
        if ("FULLTEXT".equals(content.getCaptureMode())) {
            return "FULLTEXT_ALLOWED".equals(content.getLicensePolicy());
        }
        if ("SNIPPET".equals(content.getCaptureMode())) {
            return "FULLTEXT_ALLOWED".equals(content.getLicensePolicy())
                    || "SNIPPET_ALLOWED".equals(content.getLicensePolicy());
        }
        return false;
    }

    private AiSummaryInput contentInput(RawNewsContent content) {
        String sourceType = "FULLTEXT".equals(content.getCaptureMode())
                ? "CONTENT_FULLTEXT"
                : "CONTENT_SNIPPET";
        String text = content.getContentText() == null ? "" : content.getContentText().trim();
        return new AiSummaryInput(
                sourceType,
                content.getId(),
                text,
                sha256(text),
                preview(text),
                content.getLicenseNote()
        );
    }

    private AiSummaryInput rssInput(CandidateArticle candidate) {
        String text = candidate.getSummary() == null ? "" : candidate.getSummary().trim();
        return new AiSummaryInput(
                "RSS_SUMMARY",
                null,
                text,
                sha256(text),
                preview(text),
                null
        );
    }

    private CandidateArticle requirePendingCandidate(Long id) {
        CandidateArticle candidate = candidateArticleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Candidate not found"));
        if (!PENDING_REVIEW.equals(candidate.getStatus())) {
            throw new ResponseStatusException(CONFLICT, "Candidate is not pending review");
        }
        return candidate;
    }

    private AiSummaryProvider requireProvider(String providerType) {
        AiSummaryProvider provider = providers.get(providerType);
        if (provider == null) {
            throw new ResponseStatusException(UNPROCESSABLE_ENTITY, "AI summary provider is not configured");
        }
        return provider;
    }

    private String preview(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.length() <= 500 ? value : value.substring(0, 500).trim();
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private String blankToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    private String errorMessage(RuntimeException exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
    }
}
