package com.pulsebrief.ingestion.provider;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class NewsIngestionProviderContextTest {
    @Autowired
    private List<NewsIngestionProvider> providers;

    @Test
    void registersFixtureProviderAsSpringBean() {
        assertThat(providers)
                .extracting(NewsIngestionProvider::providerType)
                .contains("FIXTURE");
    }
}
