package com.pulsebrief.ingestion.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@ConditionalOnProperty(prefix = "pulsebrief.ingestion.scheduling", name = "enabled", havingValue = "true")
public class IngestionSchedulingConfiguration {
}
