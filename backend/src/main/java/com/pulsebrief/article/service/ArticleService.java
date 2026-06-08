package com.pulsebrief.article.service;

import com.pulsebrief.article.api.ArticleCardResponse;
import com.pulsebrief.article.api.ArticleDetailResponse;
import com.pulsebrief.article.api.HomeArticlesResponse;
import java.util.List;

public interface ArticleService {
    HomeArticlesResponse getHomeArticles(String categoryCode, Integer pageSize);

    List<ArticleCardResponse> listArticles(String categoryCode, Integer page, Integer pageSize);

    ArticleDetailResponse getArticleDetail(Long id);
}
