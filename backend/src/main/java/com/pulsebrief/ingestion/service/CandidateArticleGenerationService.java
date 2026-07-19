package com.pulsebrief.ingestion.service;

import com.pulsebrief.ingestion.domain.CandidateArticle;
import com.pulsebrief.ingestion.domain.RawNewsItem;
import com.pulsebrief.ingestion.repository.CandidateArticleRepository;
import com.pulsebrief.ingestion.repository.RawNewsItemRepository;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CandidateArticleGenerationService {
    private final RawNewsItemRepository rawNewsItemRepository;
    private final CandidateArticleRepository candidateArticleRepository;
    private final CandidateClassificationService classificationService;

    public CandidateArticleGenerationService(
            RawNewsItemRepository rawNewsItemRepository,
            CandidateArticleRepository candidateArticleRepository,
            CandidateClassificationService classificationService
    ) {
        this.rawNewsItemRepository = rawNewsItemRepository;
        this.candidateArticleRepository = candidateArticleRepository;
        this.classificationService = classificationService;
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

            candidateArticleRepository.save(new CandidateArticle(rawItem, classificationService.classify(rawItem)));
            rawItem.markAsCandidate();
            generatedCount++;
        }

        return new CandidateArticleGenerationResult(rawItems.size(), generatedCount);
    }

}
