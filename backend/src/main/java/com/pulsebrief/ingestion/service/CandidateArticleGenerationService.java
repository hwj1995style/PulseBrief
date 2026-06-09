package com.pulsebrief.ingestion.service;

import com.pulsebrief.ingestion.domain.CandidateArticle;
import com.pulsebrief.ingestion.domain.RawNewsItem;
import com.pulsebrief.ingestion.repository.CandidateArticleRepository;
import com.pulsebrief.ingestion.repository.RawNewsItemRepository;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CandidateArticleGenerationService {
    private final RawNewsItemRepository rawNewsItemRepository;
    private final CandidateArticleRepository candidateArticleRepository;

    public CandidateArticleGenerationService(
            RawNewsItemRepository rawNewsItemRepository,
            CandidateArticleRepository candidateArticleRepository
    ) {
        this.rawNewsItemRepository = rawNewsItemRepository;
        this.candidateArticleRepository = candidateArticleRepository;
    }

    @Transactional
    public CandidateArticleGenerationResult generatePendingCandidates(int limit) {
        return generatePendingCandidates(null, limit);
    }

    @Transactional
    public CandidateArticleGenerationResult generatePendingCandidates(String sourceCode, int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 100);
        List<RawNewsItem> rawItems = sourceCode == null || sourceCode.isBlank()
                ? rawNewsItemRepository.findByItemStatusOrderByFetchedAtAsc("NEW", PageRequest.of(0, safeLimit))
                : rawNewsItemRepository.findBySourceCodeAndItemStatusOrderByFetchedAtAsc(
                        sourceCode,
                        "NEW",
                        PageRequest.of(0, safeLimit)
                );
        int generatedCount = 0;

        for (RawNewsItem rawItem : rawItems) {
            if (candidateArticleRepository.existsByRawNewsItem_Id(rawItem.getId())) {
                continue;
            }

            candidateArticleRepository.save(new CandidateArticle(rawItem, inferCategoryCode(rawItem)));
            rawItem.markAsCandidate();
            generatedCount++;
        }

        return new CandidateArticleGenerationResult(rawItems.size(), generatedCount);
    }

    private String inferCategoryCode(RawNewsItem rawItem) {
        String text = ((rawItem.getTitle() == null ? "" : rawItem.getTitle())
                + " "
                + (rawItem.getSummary() == null ? "" : rawItem.getSummary())
                + " "
                + (rawItem.getSourceName() == null ? "" : rawItem.getSourceName()))
                .toLowerCase(Locale.ROOT);

        if (containsAny(text, "goldman", "morgan", "nomura", "ubs", "jpmorgan", "投行", "高盛", "摩根", "野村")) {
            return "investment_view";
        }
        if (containsAny(text, "ai", "nvidia", "chip", "model", "人工智能", "英伟达", "芯片", "大模型")) {
            return "ai";
        }
        if (containsAny(text, "fed", "central bank", "rate", "inflation", "美联储", "央行", "利率", "通胀")) {
            return "macro";
        }
        if (containsAny(text, "market", "stock", "bond", "oil", "currency", "标普", "纳指", "股", "债", "油", "汇率")) {
            return "finance";
        }
        if (containsAny(text, "semiconductor", "ev", "energy", "industry", "半导体", "新能源", "产业")) {
            return "industry";
        }
        return "global";
    }

    private boolean containsAny(String value, String... keywords) {
        for (String keyword : keywords) {
            if (value.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
