package com.pulsebrief.article.api;

import com.pulsebrief.article.service.ArticleService;
import com.pulsebrief.common.api.ApiResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/articles")
public class ArticleController {
    private final ArticleService articleService;

    public ArticleController(ArticleService articleService) {
        this.articleService = articleService;
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
    public ApiResponse<ArticleDetailResponse> articleDetail(@PathVariable Long id) {
        return ApiResponse.ok(articleService.getArticleDetail(id));
    }
}
