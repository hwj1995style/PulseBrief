package com.pulsebrief.ingestion;

import com.pulsebrief.ingestion.service.CandidateClassificationProvider;
import com.pulsebrief.ingestion.service.DeepSeekClassificationProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "pulsebrief.ai.deepseek.enabled=false",
        "pulsebrief.ai.deepseek.api-key=test-only-key",
        "pulsebrief.ai.deepseek.classification.enabled=true"
})
class DeepSeekClassificationProviderContextTest {
    @Autowired
    private DeepSeekClassificationProvider provider;

    @Test
    void startsClassificationIndependentlyFromSummaryProvider() {
        assertThat(provider).isInstanceOf(CandidateClassificationProvider.class);
        assertThat(provider.providerType()).isEqualTo("DEEPSEEK");
    }
}
