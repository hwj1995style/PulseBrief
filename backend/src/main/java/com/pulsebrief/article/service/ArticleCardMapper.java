package com.pulsebrief.article.service;

import com.pulsebrief.article.api.ArticleCardResponse;
import com.pulsebrief.article.domain.NewsArticle;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;

@Component
public class ArticleCardMapper {
    private static final DateTimeFormatter API_TIME = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    public ArticleCardResponse toCard(NewsArticle article) {
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

    public ArticleCardResponse toFavoriteCard(NewsArticle article) {
        ArticleCardResponse card = toCard(article);
        return new ArticleCardResponse(
                card.id(),
                card.title(),
                card.sourceName(),
                card.publishTime(),
                card.categoryCode(),
                card.categoryName(),
                card.summary(),
                card.imageUrl(),
                card.audioDuration(),
                card.hot(),
                card.breaking(),
                true
        );
    }

    public String formatTime(NewsArticle article) {
        if (article.getPublishTime() == null) {
            return null;
        }
        return article.getPublishTime().atOffset(ZoneOffset.ofHours(8)).format(API_TIME);
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
