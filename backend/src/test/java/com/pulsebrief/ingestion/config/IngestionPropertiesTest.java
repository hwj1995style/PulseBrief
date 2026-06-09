package com.pulsebrief.ingestion.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class IngestionPropertiesTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfiguration.class);

    @Test
    void defaultsToDisabledWithoutSources() {
        contextRunner.run(context -> {
            IngestionProperties properties = context.getBean(IngestionProperties.class);

            assertThat(properties.enabled()).isFalse();
            assertThat(properties.sources()).isEmpty();
        });
    }

    @Test
    void bindsSourceConfigurationFromProperties() {
        contextRunner
                .withPropertyValues(
                        "pulsebrief.ingestion.enabled=true",
                        "pulsebrief.ingestion.sources[0].code=fixture-global",
                        "pulsebrief.ingestion.sources[0].name=Fixture Global",
                        "pulsebrief.ingestion.sources[0].provider-type=FIXTURE",
                        "pulsebrief.ingestion.sources[0].base-url=classpath:fixtures/news.json",
                        "pulsebrief.ingestion.sources[0].default-category-code=global",
                        "pulsebrief.ingestion.sources[0].enabled=false",
                        "pulsebrief.ingestion.sources[0].rate-limit-per-hour=30"
                )
                .run(context -> {
                    IngestionProperties properties = context.getBean(IngestionProperties.class);

                    assertThat(properties.enabled()).isTrue();
                    assertThat(properties.sources()).hasSize(1);
                    assertThat(properties.sources().get(0).code()).isEqualTo("fixture-global");
                    assertThat(properties.sources().get(0).providerType()).isEqualTo("FIXTURE");
                    assertThat(properties.sources().get(0).enabled()).isFalse();
                    assertThat(properties.sources().get(0).rateLimitPerHour()).isEqualTo(30);
                });
    }

    @EnableConfigurationProperties(IngestionProperties.class)
    static class TestConfiguration {
    }
}
