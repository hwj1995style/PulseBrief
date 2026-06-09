package com.pulsebrief.readhistory.service;

import com.pulsebrief.article.api.ArticleCardResponse;
import com.pulsebrief.article.service.ArticleCardMapper;
import com.pulsebrief.common.api.PageResponse;
import com.pulsebrief.readhistory.api.ReadHistoryRecordResponse;
import com.pulsebrief.readhistory.domain.UserReadHistory;
import com.pulsebrief.readhistory.repository.UserReadHistoryRepository;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
public class ReadHistoryApplicationService implements ReadHistoryService {
    private final UserReadHistoryRepository readHistoryRepository;
    private final ArticleCardMapper articleCardMapper;

    public ReadHistoryApplicationService(
            UserReadHistoryRepository readHistoryRepository,
            ArticleCardMapper articleCardMapper
    ) {
        this.readHistoryRepository = readHistoryRepository;
        this.articleCardMapper = articleCardMapper;
    }

    @Override
    public ReadHistoryRecordResponse recordReadHistory(Long userId, Long articleId) {
        UserReadHistory saved = readHistoryRepository.save(new UserReadHistory(userId, articleId));
        return new ReadHistoryRecordResponse(saved.getId());
    }

    @Override
    public PageResponse<ArticleCardResponse> listReadHistory(Long userId, Integer page, Integer pageSize) {
        PageRequest pageable = PageRequest.of(safePage(page), safePageSize(pageSize));
        List<ArticleCardResponse> items = readHistoryRepository.findReadArticles(userId, pageable)
                .stream()
                .map(articleCardMapper::toCard)
                .toList();
        return PageResponse.of(items, page, pageSize, readHistoryRepository.countByUserId(userId).longValue());
    }

    @Override
    public Boolean clearReadHistory(Long userId) {
        readHistoryRepository.deleteByUserId(userId);
        return true;
    }

    private int safePage(Integer page) {
        return Math.max(page == null ? 1 : page, 1) - 1;
    }

    private int safePageSize(Integer pageSize) {
        return Math.min(Math.max(pageSize == null ? 20 : pageSize, 1), 50);
    }
}
