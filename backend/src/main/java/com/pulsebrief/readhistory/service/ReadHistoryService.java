package com.pulsebrief.readhistory.service;

import com.pulsebrief.article.api.ArticleCardResponse;
import com.pulsebrief.common.api.PageResponse;
import com.pulsebrief.readhistory.api.ReadHistoryRecordResponse;

public interface ReadHistoryService {
    ReadHistoryRecordResponse recordReadHistory(Long userId, Long articleId);

    PageResponse<ArticleCardResponse> listReadHistory(Long userId, Integer page, Integer pageSize);

    Boolean clearReadHistory(Long userId);
}
