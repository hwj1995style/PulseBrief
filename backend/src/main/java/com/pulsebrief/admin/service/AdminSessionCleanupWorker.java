package com.pulsebrief.admin.service;

import com.pulsebrief.admin.config.AdminSecurityProperties;
import com.pulsebrief.admin.repository.AdminSessionRepository;
import java.time.LocalDateTime;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(
        prefix = "pulsebrief.admin.security",
        name = "session-cleanup-enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class AdminSessionCleanupWorker {
    private final AdminSessionRepository sessionRepository;
    private final AdminSecurityProperties properties;

    public AdminSessionCleanupWorker(
            AdminSessionRepository sessionRepository,
            AdminSecurityProperties properties
    ) {
        this.sessionRepository = sessionRepository;
        this.properties = properties;
    }

    @Scheduled(cron = "${pulsebrief.admin.security.session-cleanup-cron:0 15 3 * * *}")
    @Transactional
    public int cleanup() {
        LocalDateTime now = LocalDateTime.now();
        return sessionRepository.deleteExpiredOrOldRevoked(
                now,
                now.minusDays(properties.sessionCleanupRetentionDays())
        );
    }
}
