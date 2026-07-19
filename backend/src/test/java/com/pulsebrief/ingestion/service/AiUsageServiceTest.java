package com.pulsebrief.ingestion.service;

import com.pulsebrief.ingestion.config.AiUsageProperties;
import com.pulsebrief.ingestion.domain.AiUsageEvent;
import com.pulsebrief.ingestion.repository.AiUsageEventRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiUsageServiceTest {
    private final AiUsageEventRepository repository = mock(AiUsageEventRepository.class);
    private final AiUsageProperties properties = new AiUsageProperties(
            true,
            2,
            1000,
            80,
            new BigDecimal("1.50"),
            new BigDecimal("2.00")
    );
    private final AiUsageService service = new AiUsageService(repository, properties);

    @Test
    void reservesRequestAndCalculatesConfiguredCost() {
        when(repository.findByCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDesc(any(), any()))
                .thenReturn(List.of());
        doAnswer(invocation -> withId(invocation.getArgument(0), 10L)).when(repository).save(any(AiUsageEvent.class));

        Long eventId = service.begin("SUMMARY", "DEEPSEEK", "deepseek-v4-flash");
        AiUsageEvent event = new AiUsageEvent("SUMMARY", "DEEPSEEK", "deepseek-v4-flash", "RUNNING");
        withId(event, eventId);
        when(repository.findById(eventId)).thenReturn(Optional.of(event));

        service.markSuccess(eventId, "DEEPSEEK", 1000, 500);

        assertThat(event.getRequestStatus()).isEqualTo("SUCCESS");
        assertThat(event.getPromptTokens()).isEqualTo(1000);
        assertThat(event.getCompletionTokens()).isEqualTo(500);
        assertThat(event.getEstimatedCostUsd()).isEqualByComparingTo("0.00250000");
    }

    @Test
    void blocksBeforeCallingProviderWhenDailyRequestLimitIsReached() {
        AiUsageEvent first = successfulEvent();
        AiUsageEvent second = successfulEvent();
        when(repository.findByCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDesc(any(), any()))
                .thenReturn(List.of(first, second));

        assertThatThrownBy(() -> service.begin("CLASSIFICATION", "DEEPSEEK", "deepseek-v4-flash"))
                .isInstanceOf(AiUsageLimitExceededException.class)
                .hasMessageContaining("request limit");

        verify(repository).save(any(AiUsageEvent.class));
    }

    @Test
    void raisesWarningAtConfiguredThreshold() {
        AiUsageEvent event = new AiUsageEvent("SUMMARY", "DEEPSEEK", "deepseek-v4-flash", "RUNNING");
        event.markSuccess(800, 0, BigDecimal.ZERO);
        when(repository.findByCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDesc(any(), any()))
                .thenReturn(List.of(event));

        AiUsageSnapshot snapshot = service.todaySnapshot();

        assertThat(snapshot.requestCount()).isEqualTo(1);
        assertThat(snapshot.promptTokens()).isEqualTo(800);
        assertThat(snapshot.alertLevel()).isEqualTo("WARNING");
    }

    private AiUsageEvent successfulEvent() {
        AiUsageEvent event = new AiUsageEvent("SUMMARY", "DEEPSEEK", "deepseek-v4-flash", "RUNNING");
        event.markSuccess(10, 5, BigDecimal.ZERO);
        return event;
    }

    private AiUsageEvent withId(AiUsageEvent event, Long id) {
        ReflectionTestUtils.setField(event, "id", id);
        return event;
    }
}
