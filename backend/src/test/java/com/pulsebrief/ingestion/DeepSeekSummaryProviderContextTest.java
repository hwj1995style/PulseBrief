package com.pulsebrief.ingestion;

import com.pulsebrief.ingestion.service.AiSummaryProvider;
import com.pulsebrief.ingestion.service.DeepSeekSummaryProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "pulsebrief.ai.deepseek.enabled=true",
        "pulsebrief.ai.deepseek.api-key=test-only-key"
})
class DeepSeekSummaryProviderContextTest {
    @Autowired
    private DeepSeekSummaryProvider provider;

    @Test
    void startsWhenDeepSeekIsEnabled() {
        assertThat(provider).isInstanceOf(AiSummaryProvider.class);
        assertThat(provider.providerType()).isEqualTo("DEEPSEEK");
    }
}
