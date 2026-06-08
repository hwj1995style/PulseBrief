package com.pulsebrief.article.service;

import com.pulsebrief.article.api.ArticleCardResponse;
import com.pulsebrief.article.api.ArticleDetailResponse;
import com.pulsebrief.article.api.DigestHeroResponse;
import com.pulsebrief.article.api.HomeArticlesResponse;
import com.pulsebrief.article.domain.NewsArticle;
import com.pulsebrief.article.repository.ArticleRepository;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class ArticleQueryService implements ArticleService {
    private static final String PUBLISHED = "PUBLISHED";
    private static final DateTimeFormatter API_TIME = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private final ArticleRepository articleRepository;

    public ArticleQueryService(ArticleRepository articleRepository) {
        this.articleRepository = articleRepository;
    }

    @Override
    public HomeArticlesResponse getHomeArticles(String categoryCode, Integer pageSize) {
        List<ArticleCardResponse> articles = listArticles(categoryCode, 1, pageSize);
        ArticleCardResponse investmentPick = articleRepository
                .findFirstByArticleStatusAndCategoryCodeOrderByHotScoreDescPublishTimeDesc(PUBLISHED, "investment_view")
                .map(this::toCard)
                .orElse(articles.isEmpty() ? null : articles.get(0));

        return new HomeArticlesResponse(
                new DigestHeroResponse(1L, "今日全球简报", "精选 10 条全球重点资讯"),
                investmentPick,
                articles
        );
    }

    @Override
    public List<ArticleCardResponse> listArticles(String categoryCode, Integer page, Integer pageSize) {
        int safePage = Math.max(page == null ? 1 : page, 1) - 1;
        int safePageSize = Math.min(Math.max(pageSize == null ? 20 : pageSize, 1), 50);
        PageRequest pageable = PageRequest.of(safePage, safePageSize);
        List<NewsArticle> articles = "all".equalsIgnoreCase(categoryCode)
                ? articleRepository.findByArticleStatusOrderByTopDescHotScoreDescPublishTimeDesc(PUBLISHED, pageable)
                : articleRepository.findByArticleStatusAndCategoryCodeOrderByTopDescHotScoreDescPublishTimeDesc(
                        PUBLISHED,
                        categoryCode,
                        pageable
                );
        return articles.stream().map(this::toCard).toList();
    }

    @Override
    public ArticleDetailResponse getArticleDetail(Long id) {
        NewsArticle article = articleRepository.findByIdAndArticleStatus(id, PUBLISHED)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Article not found"));
        List<ArticleCardResponse> related = articleRepository
                .findByArticleStatusAndCategoryCodeOrderByTopDescHotScoreDescPublishTimeDesc(
                        PUBLISHED,
                        article.getCategoryCode(),
                        PageRequest.of(0, 3)
                )
                .stream()
                .filter(item -> !item.getId().equals(article.getId()))
                .map(this::toCard)
                .toList();
        return new ArticleDetailResponse(
                article.getId(),
                article.getTitle(),
                article.getSourceName(),
                formatTime(article),
                article.getCategoryCode(),
                categoryName(article.getCategoryCode()),
                article.getAiSummary(),
                parseKeyPoints(article.getKeyPoints()),
                article.getImpactAnalysis(),
                article.getOriginalUrl(),
                false,
                related
        );
    }

    private ArticleCardResponse toCard(NewsArticle article) {
        return new ArticleCardResponse(
                article.getId(),
                article.getTitle(),
                article.getSourceName(),
                formatTime(article),
                article.getCategoryCode(),
                categoryName(article.getCategoryCode()),
                article.getSummary(),
                "",
                audioDuration(article.getId()),
                article.getHotScore() != null && article.getHotScore().doubleValue() >= 90,
                article.getTop() != null && article.getTop() == 1,
                false
        );
    }

    private String formatTime(NewsArticle article) {
        if (article.getPublishTime() == null) {
            return null;
        }
        return article.getPublishTime().atOffset(ZoneOffset.ofHours(8)).format(API_TIME);
    }

    private List<String> parseKeyPoints(String keyPoints) {
        if (keyPoints == null || keyPoints.isBlank()) {
            return List.of();
        }
        return Arrays.stream(keyPoints.split("\\R"))
                .map(String::trim)
                .filter(point -> !point.isEmpty())
                .toList();
    }

    private String audioDuration(Long id) {
        return switch (id.intValue() % 5) {
            case 0 -> "02:05";
            case 1 -> "02:48";
            case 2 -> "02:12";
            case 3 -> "02:36";
            default -> "02:22";
        };
    }

    private String categoryName(String code) {
        return switch (code) {
            case "global" -> "全球热点";
            case "finance" -> "财经市场";
            case "tech" -> "科技趋势";
            case "ai" -> "AI 前沿";
            case "macro" -> "宏观政策";
            case "investment_view" -> "投行观点";
            case "industry" -> "产业观察";
            case "company" -> "公司动态";
            default -> "全球资讯";
        };
    }
}
