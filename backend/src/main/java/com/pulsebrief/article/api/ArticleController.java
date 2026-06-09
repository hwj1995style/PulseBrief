package com.pulsebrief.article.api;

import com.pulsebrief.article.service.ArticleService;
import com.pulsebrief.common.api.ApiResponse;
import com.pulsebrief.common.security.DevTokenSupport;
import com.pulsebrief.readhistory.service.ReadHistoryService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/articles")
public class ArticleController {
    private final ArticleService articleService;
    private final ReadHistoryService readHistoryService;

    public ArticleController(ArticleService articleService, ReadHistoryService readHistoryService) {
        this.articleService = articleService;
        this.readHistoryService = readHistoryService;
    }

    @GetMapping("/home")
    public ApiResponse<HomeArticlesResponse> homeArticles(
            @RequestParam(defaultValue = "all") String categoryCode,
            @RequestParam(defaultValue = "20") Integer pageSize
    ) {
        return ApiResponse.ok(articleService.getHomeArticles(categoryCode, pageSize));
    }

    @GetMapping
    public ApiResponse<List<ArticleCardResponse>> articles(
            @RequestParam(defaultValue = "all") String categoryCode,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize
    ) {
        return ApiResponse.ok(articleService.listArticles(categoryCode, page, pageSize));
    }

    @GetMapping("/{id}")
    public ApiResponse<ArticleDetailResponse> articleDetail(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        recordReadHistoryIfLoggedIn(authorization, id);
        return ApiResponse.ok(articleService.getArticleDetail(id));
    }

    private void recordReadHistoryIfLoggedIn(String authorization, Long articleId) {
        if (authorization == null || authorization.isBlank()) {
            return;
        }
        try {
            Long userId = DevTokenSupport.requireUserId(authorization);
            readHistoryService.recordReadHistory(userId, articleId);
        } catch (RuntimeException ignored) {
            // Article detail remains public; malformed dev tokens should not block reading.
        }
    }
}
