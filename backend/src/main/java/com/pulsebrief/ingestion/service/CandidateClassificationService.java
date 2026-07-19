package com.pulsebrief.ingestion.service;

import com.pulsebrief.ingestion.domain.NewsIngestionSource;
import com.pulsebrief.ingestion.domain.RawNewsItem;
import com.pulsebrief.ingestion.repository.NewsIngestionSourceRepository;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class CandidateClassificationService {
    private static final Logger log = LoggerFactory.getLogger(CandidateClassificationService.class);
    private static final List<Rule> RULES = List.of(
            new Rule("investment_view", 0.96, "KEYWORD_INVESTMENT_VIEW", List.of(
                    "goldman", "morgan stanley", "nomura", "ubs", "jpmorgan", "投行", "高盛", "摩根士丹利", "野村"
            )),
            new Rule("ai", 0.94, "KEYWORD_AI", List.of(
                    "artificial intelligence", "generative ai", "nvidia", "large language model", "人工智能", "英伟达", "大模型", "算力"
            )),
            new Rule("technology", 0.88, "KEYWORD_TECHNOLOGY", List.of(
                    "technology", "software", "cloud", "semiconductor", "chip", "科技", "软件", "云计算", "半导体", "芯片"
            )),
            new Rule("macro", 0.92, "KEYWORD_MACRO", List.of(
                    "federal reserve", "central bank", "interest rate", "inflation", "美联储", "央行", "利率", "通胀"
            )),
            new Rule("finance", 0.86, "KEYWORD_FINANCE", List.of(
                    "market", "stock", "bond", "currency", "equity", "股市", "股票", "债券", "汇率"
            )),
            new Rule("industry", 0.82, "KEYWORD_INDUSTRY", List.of(
                    "electric vehicle", "renewable energy", "industry", "新能源", "电动车", "产业"
            ))
    );

    private final NewsIngestionSourceRepository sourceRepository;
    private final List<CandidateClassificationProvider> modelProviders;

    public CandidateClassificationService(
            NewsIngestionSourceRepository sourceRepository,
            List<CandidateClassificationProvider> modelProviders
    ) {
        this.sourceRepository = sourceRepository;
        this.modelProviders = List.copyOf(modelProviders);
    }

    public ClassificationDecision classify(RawNewsItem rawItem) {
        String text = String.join(" ", safe(rawItem.getTitle()), safe(rawItem.getSummary()), safe(rawItem.getSourceName()))
                .toLowerCase(Locale.ROOT);
        for (Rule rule : RULES) {
            String keyword = rule.firstMatch(text);
            if (keyword != null) {
                return new ClassificationDecision(rule.categoryCode(), rule.confidence(), rule.code() + ":" + keyword);
            }
        }

        boolean modelAttempted = false;
        for (CandidateClassificationProvider provider : modelProviders) {
            modelAttempted = true;
            try {
                var decision = provider.classify(rawItem);
                if (decision.isPresent()) {
                    return decision.get();
                }
            } catch (RuntimeException exception) {
                log.warn(
                        "Candidate model classification failed; provider={}, sourceCode={}, fallback=rules, error={}",
                        provider.providerType(),
                        rawItem.getSourceCode(),
                        exception.toString()
                );
            }
        }

        String sourceFallbackRule = modelAttempted ? "MODEL_FALLBACK_SOURCE_DEFAULT" : "SOURCE_DEFAULT";
        String globalFallbackRule = modelAttempted ? "MODEL_FALLBACK_GLOBAL" : "GLOBAL_FALLBACK";
        return sourceRepository.findByCode(rawItem.getSourceCode())
                .map(NewsIngestionSource::getDefaultCategoryCode)
                .filter(value -> !value.isBlank())
                .map(category -> new ClassificationDecision(
                        category,
                        0.55,
                        sourceFallbackRule
                ))
                .orElseGet(() -> new ClassificationDecision(
                        "global",
                        0.30,
                        globalFallbackRule
                ));
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private record Rule(String categoryCode, double confidence, String code, List<String> keywords) {
        private String firstMatch(String text) {
            return keywords.stream().filter(text::contains).findFirst().orElse(null);
        }
    }
}
