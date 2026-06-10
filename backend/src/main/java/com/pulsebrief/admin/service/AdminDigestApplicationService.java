package com.pulsebrief.admin.service;

import com.pulsebrief.admin.api.AdminDigestArticleRequest;
import com.pulsebrief.admin.api.AdminDigestArticleResponse;
import com.pulsebrief.admin.api.AdminDigestCreateRequest;
import com.pulsebrief.admin.api.AdminDigestResponse;
import com.pulsebrief.article.api.ArticleCardResponse;
import com.pulsebrief.article.domain.NewsArticle;
import com.pulsebrief.article.repository.ArticleRepository;
import com.pulsebrief.article.service.ArticleCardMapper;
import com.pulsebrief.common.api.PageResponse;
import com.pulsebrief.digest.domain.DailyDigest;
import com.pulsebrief.digest.domain.DailyDigestArticle;
import com.pulsebrief.digest.repository.DailyDigestArticleRepository;
import com.pulsebrief.digest.repository.DigestRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;

@Service
public class AdminDigestApplicationService {
    private static final String DRAFT = "DRAFT";
    private static final String PUBLISHED = "PUBLISHED";
    private static final DateTimeFormatter API_TIME = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private final DigestRepository digestRepository;
    private final DailyDigestArticleRepository digestArticleRepository;
    private final ArticleRepository articleRepository;
    private final ArticleCardMapper articleCardMapper;

    public AdminDigestApplicationService(
            DigestRepository digestRepository,
            DailyDigestArticleRepository digestArticleRepository,
            ArticleRepository articleRepository,
            ArticleCardMapper articleCardMapper
    ) {
        this.digestRepository = digestRepository;
        this.digestArticleRepository = digestArticleRepository;
        this.articleRepository = articleRepository;
        this.articleCardMapper = articleCardMapper;
    }

    @Transactional(readOnly = true)
    public PageResponse<ArticleCardResponse> listArticleCandidates(
            String keyword,
            String categoryCode,
            Integer page,
            Integer pageSize
    ) {
        int safePage = Math.max(page == null ? 1 : page, 1);
        int safePageSize = Math.min(Math.max(pageSize == null ? 20 : pageSize, 1), 50);
        String safeKeyword = keyword == null ? "" : keyword.trim().toLowerCase();
        String safeCategory = categoryCode == null || categoryCode.isBlank() ? "all" : categoryCode;
        List<NewsArticle> matched = articleRepository.findAll().stream()
                .filter(article -> PUBLISHED.equals(article.getArticleStatus()))
                .filter(article -> "all".equals(safeCategory) || safeCategory.equals(article.getCategoryCode()))
                .filter(article -> matchesKeyword(article, safeKeyword))
                .sorted(Comparator.comparing(NewsArticle::getPublishTime, Comparator.nullsLast(Comparator.naturalOrder()))
                        .reversed())
                .toList();
        int from = Math.min((safePage - 1) * safePageSize, matched.size());
        int to = Math.min(from + safePageSize, matched.size());
        return PageResponse.of(
                matched.subList(from, to).stream().map(articleCardMapper::toCard).toList(),
                safePage,
                safePageSize,
                (long) matched.size()
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminDigestResponse> listDigests(String status, Integer page, Integer pageSize) {
        int safePage = Math.max(page == null ? 1 : page, 1);
        int safePageSize = Math.min(Math.max(pageSize == null ? 20 : pageSize, 1), 50);
        String safeStatus = status == null || status.isBlank() ? DRAFT : status;
        Page<DailyDigest> digestPage = digestRepository.findByDigestStatusOrderByDigestDateDescPublishTimeDescIdDesc(
                safeStatus,
                PageRequest.of(safePage - 1, safePageSize)
        );
        return PageResponse.of(
                digestPage.getContent().stream().map(this::toResponse).toList(),
                safePage,
                safePageSize,
                digestPage.getTotalElements()
        );
    }

    @Transactional(readOnly = true)
    public AdminDigestResponse getDigest(Long id) {
        return toResponse(requireDigest(id));
    }

    @Transactional
    public AdminDigestResponse createDigest(AdminDigestCreateRequest request) {
        validateRequest(request);
        List<AdminDigestArticleRequest> articleRequests = normalizedArticles(request.articles());
        assertArticlesPublished(articleRequests);
        String content = blankToDefault(request.content(), contentFromArticles(articleRequests));
        DailyDigest digest = digestRepository.save(new DailyDigest(
                LocalDate.parse(request.digestDate()),
                request.digestType(),
                request.categoryCode(),
                request.title().trim(),
                request.summary(),
                content,
                request.audioText()
        ));
        replaceDigestArticles(digest.getId(), articleRequests);
        return toResponse(digest);
    }

    @Transactional
    public AdminDigestResponse publishDigest(Long id) {
        DailyDigest digest = requireDigest(id);
        if (!DRAFT.equals(digest.getDigestStatus())) {
            throw new ResponseStatusException(CONFLICT, "Digest is not draft");
        }
        if (digestRepository.existsByDigestDateAndDigestTypeAndDigestStatus(
                digest.getDigestDate(),
                digest.getDigestType(),
                PUBLISHED
        )) {
            throw new ResponseStatusException(CONFLICT, "Published digest already exists for date and type");
        }
        digest.publish(LocalDateTime.now());
        return toResponse(digest);
    }

    private void validateRequest(AdminDigestCreateRequest request) {
        if (request == null) {
            throw new ResponseStatusException(UNPROCESSABLE_ENTITY, "Digest request is required");
        }
        if (request.digestDate() == null || request.digestDate().isBlank()) {
            throw new ResponseStatusException(UNPROCESSABLE_ENTITY, "Digest date is required");
        }
        if (request.digestType() == null || request.digestType().isBlank()) {
            throw new ResponseStatusException(UNPROCESSABLE_ENTITY, "Digest type is required");
        }
        if (request.title() == null || request.title().isBlank()) {
            throw new ResponseStatusException(UNPROCESSABLE_ENTITY, "Digest title is required");
        }
    }

    private List<AdminDigestArticleRequest> normalizedArticles(List<AdminDigestArticleRequest> articles) {
        if (articles == null || articles.isEmpty()) {
            throw new ResponseStatusException(UNPROCESSABLE_ENTITY, "Digest articles are required");
        }
        if (articles.size() > 10) {
            throw new ResponseStatusException(UNPROCESSABLE_ENTITY, "Digest supports at most 10 articles");
        }
        return articles.stream()
                .sorted(Comparator.comparing(item -> item.sortNo() == null ? 0 : item.sortNo()))
                .toList();
    }

    private void assertArticlesPublished(List<AdminDigestArticleRequest> articles) {
        for (AdminDigestArticleRequest item : articles) {
            if (item.articleId() == null) {
                throw new ResponseStatusException(UNPROCESSABLE_ENTITY, "Article id is required");
            }
            NewsArticle article = articleRepository.findById(item.articleId())
                    .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Article not found"));
            if (!PUBLISHED.equals(article.getArticleStatus())) {
                throw new ResponseStatusException(CONFLICT, "Digest article is not published");
            }
        }
    }

    private void replaceDigestArticles(Long digestId, List<AdminDigestArticleRequest> articles) {
        digestArticleRepository.deleteByDigestId(digestId);
        digestArticleRepository.saveAll(articles.stream()
                .map(item -> new DailyDigestArticle(
                        digestId,
                        item.articleId(),
                        item.sortNo(),
                        item.highlightText()
                ))
                .toList());
    }

    private AdminDigestResponse toResponse(DailyDigest digest) {
        List<DailyDigestArticle> links = digestArticleRepository.findByDigestIdOrderBySortNoAscIdAsc(digest.getId());
        Map<Long, NewsArticle> articleById = articleRepository.findAllById(
                        links.stream().map(DailyDigestArticle::getArticleId).toList()
                )
                .stream()
                .collect(Collectors.toMap(NewsArticle::getId, Function.identity()));
        List<AdminDigestArticleResponse> articles = links.stream()
                .map(link -> {
                    NewsArticle article = articleById.get(link.getArticleId());
                    return new AdminDigestArticleResponse(
                            link.getArticleId(),
                            link.getSortNo(),
                            link.getHighlightText(),
                            article == null ? "" : article.getTitle(),
                            article == null ? "" : article.getSourceName()
                    );
                })
                .toList();
        return new AdminDigestResponse(
                digest.getId(),
                digest.getDigestDate().toString(),
                digest.getDigestType(),
                digest.getCategoryCode(),
                digest.getTitle(),
                digest.getSummary(),
                digest.getContent(),
                digest.getAudioText(),
                digest.getDigestStatus(),
                formatTime(digest.getPublishTime()),
                (long) articles.size(),
                articles,
                availableActions(digest)
        );
    }

    private DailyDigest requireDigest(Long id) {
        return digestRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Digest not found"));
    }

    private boolean matchesKeyword(NewsArticle article, String keyword) {
        if (keyword.isBlank()) {
            return true;
        }
        return contains(article.getTitle(), keyword)
                || contains(article.getSummary(), keyword)
                || contains(article.getSourceName(), keyword);
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.toLowerCase().contains(keyword);
    }

    private String contentFromArticles(List<AdminDigestArticleRequest> articles) {
        return articles.stream()
                .map(item -> blankToDefault(item.highlightText(), "Article " + item.articleId()))
                .collect(Collectors.joining(System.lineSeparator()));
    }

    private String blankToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    private List<String> availableActions(DailyDigest digest) {
        if (DRAFT.equals(digest.getDigestStatus())) {
            return List.of("EDIT", "PUBLISH");
        }
        if (PUBLISHED.equals(digest.getDigestStatus())) {
            return List.of("OFFLINE");
        }
        return List.of();
    }

    private String formatTime(LocalDateTime value) {
        if (value == null) {
            return null;
        }
        return value.atOffset(ZoneOffset.ofHours(8)).format(API_TIME);
    }
}
