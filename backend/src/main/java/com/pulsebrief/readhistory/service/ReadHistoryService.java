package com.pulsebrief.readhistory.service;

import com.pulsebrief.article.api.ArticleCardResponse;
import com.pulsebrief.readhistory.api.ReadHistoryRecordResponse;
import java.util.List;

public interface ReadHistoryService {
    ReadHistoryRecordResponse recordReadHistory(Long userId, Long articleId);

    List<ArticleCardResponse> listReadHistory(Long userId, Integer page, Integer pageSize);
}
