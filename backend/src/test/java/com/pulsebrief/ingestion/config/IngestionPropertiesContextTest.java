package com.pulsebrief.ingestion.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class IngestionPropertiesContextTest {
    @Autowired
    private IngestionProperties properties;

    @Test
    void registersIngestionPropertiesInApplicationContext() {
        assertThat(properties.enabled()).isFalse();
        assertThat(properties.sources()).isEmpty();
    }
}
