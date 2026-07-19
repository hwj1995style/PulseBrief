package com.pulsebrief.ingestion.service;

import com.pulsebrief.ingestion.config.AiUsageProperties;
import com.pulsebrief.ingestion.domain.AiUsageEvent;
import com.pulsebrief.ingestion.repository.AiUsageEventRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AiUsageService {
    private static final BigDecimal ONE_MILLION = BigDecimal.valueOf(1_000_000);

    private final AiUsageEventRepository repository;
    private final AiUsageProperties properties;

    public AiUsageService(AiUsageEventRepository repository, AiUsageProperties properties) {
        this.repository = repository;
        this.properties = properties;
    }

    @Transactional(
            propagation = Propagation.REQUIRES_NEW,
            noRollbackFor = AiUsageLimitExceededException.class
    )
    public Long begin(String operationType, String providerType, String modelName) {
        if (!properties.enabled()) {
            return null;
        }
        AiUsageSnapshot snapshot = snapshotInternal();
        long totalTokens = snapshot.promptTokens() + snapshot.completionTokens();
        if (snapshot.requestCount() >= properties.dailyRequestLimit()) {
            repository.save(new AiUsageEvent(operationType, providerType, modelName, "BLOCKED"));
            throw new AiUsageLimitExceededException("Daily AI request limit reached");
        }
        if (totalTokens >= properties.dailyTokenLimit()) {
            repository.save(new AiUsageEvent(operationType, providerType, modelName, "BLOCKED"));
            throw new AiUsageLimitExceededException("Daily AI token limit reached");
        }
        return repository.save(new AiUsageEvent(operationType, providerType, modelName, "RUNNING")).getId();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markSuccess(Long eventId, String providerType, int promptTokens, int completionTokens) {
        if (eventId == null) {
            return;
        }
        AiUsageEvent event = repository.findById(eventId).orElseThrow();
        event.markSuccess(promptTokens, completionTokens, estimateCost(providerType, promptTokens, completionTokens));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(Long eventId, RuntimeException exception) {
        if (eventId == null) {
            return;
        }
        AiUsageEvent event = repository.findById(eventId).orElseThrow();
        event.markFailed(exception.getClass().getSimpleName());
    }

    @Transactional(readOnly = true)
    public AiUsageSnapshot todaySnapshot() {
        return snapshotInternal();
    }

    private AiUsageSnapshot snapshotInternal() {
        LocalDate today = LocalDate.now();
        List<AiUsageEvent> events = repository
                .findByCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDesc(
                        today.atStartOfDay(),
                        today.plusDays(1).atStartOfDay()
                );
        long success = count(events, "SUCCESS");
        long failed = count(events, "FAILED");
        long running = count(events, "RUNNING");
        long blocked = count(events, "BLOCKED");
        long promptTokens = events.stream().mapToLong(event -> safe(event.getPromptTokens())).sum();
        long completionTokens = events.stream().mapToLong(event -> safe(event.getCompletionTokens())).sum();
        BigDecimal cost = events.stream()
                .map(AiUsageEvent::getEstimatedCostUsd)
                .filter(value -> value != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(8, RoundingMode.HALF_UP);
        long requests = success + failed + running;
        return new AiUsageSnapshot(
                requests,
                success,
                failed,
                blocked,
                promptTokens,
                completionTokens,
                cost,
                properties.dailyRequestLimit(),
                properties.dailyTokenLimit(),
                properties.warningPercent(),
                alertLevel(requests, promptTokens + completionTokens, blocked)
        );
    }

    private String alertLevel(long requests, long tokens, long blocked) {
        if (blocked > 0 || requests >= properties.dailyRequestLimit() || tokens >= properties.dailyTokenLimit()) {
            return "LIMIT_REACHED";
        }
        double requestPercent = requests * 100.0 / properties.dailyRequestLimit();
        double tokenPercent = tokens * 100.0 / properties.dailyTokenLimit();
        return Math.max(requestPercent, tokenPercent) >= properties.warningPercent() ? "WARNING" : "NORMAL";
    }

    private BigDecimal estimateCost(String providerType, int promptTokens, int completionTokens) {
        if (!"DEEPSEEK".equalsIgnoreCase(providerType)) {
            return BigDecimal.ZERO;
        }
        BigDecimal inputCost = BigDecimal.valueOf(Math.max(promptTokens, 0))
                .multiply(properties.deepSeekInputCostPerMillionUsd())
                .divide(ONE_MILLION, 8, RoundingMode.HALF_UP);
        BigDecimal outputCost = BigDecimal.valueOf(Math.max(completionTokens, 0))
                .multiply(properties.deepSeekOutputCostPerMillionUsd())
                .divide(ONE_MILLION, 8, RoundingMode.HALF_UP);
        return inputCost.add(outputCost).setScale(8, RoundingMode.HALF_UP);
    }

    private long count(List<AiUsageEvent> events, String status) {
        return events.stream().filter(event -> status.equals(event.getRequestStatus())).count();
    }

    private int safe(Integer value) {
        return value == null ? 0 : Math.max(value, 0);
    }
}
