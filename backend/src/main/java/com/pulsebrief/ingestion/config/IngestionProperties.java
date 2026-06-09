package com.pulsebrief.ingestion.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pulsebrief.ingestion")
public record IngestionProperties(
        Boolean enabled,
        List<Source> sources
) {
    public IngestionProperties {
        enabled = Boolean.TRUE.equals(enabled);
        sources = sources == null ? List.of() : List.copyOf(sources);
    }

    public record Source(
            String code,
            String name,
            String providerType,
            String baseUrl,
            String defaultCategoryCode,
            Boolean enabled,
            Integer rateLimitPerHour
    ) {
        public Source {
            enabled = enabled == null || enabled;
            rateLimitPerHour = rateLimitPerHour == null ? 60 : rateLimitPerHour;
        }
    }
}
