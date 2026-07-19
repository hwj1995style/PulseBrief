package com.pulsebrief.admin;

import com.pulsebrief.admin.domain.AdminSession;
import com.pulsebrief.admin.domain.AdminUser;
import com.pulsebrief.admin.repository.AdminSessionRepository;
import com.pulsebrief.admin.repository.AdminUserRepository;
import com.pulsebrief.admin.service.AdminSessionCleanupWorker;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class AdminSessionCleanupWorkerTest {
    @Autowired AdminUserRepository userRepository;
    @Autowired AdminSessionRepository sessionRepository;
    @Autowired AdminSessionCleanupWorker cleanupWorker;

    @Test
    @Transactional
    void deletesExpiredSessions() {
        AdminUser user = userRepository.save(new AdminUser(
                "cleanup-" + UUID.randomUUID(),
                new BCryptPasswordEncoder(12).encode("Strong-Test-Password-2026!"),
                "Cleanup Test",
                "VIEWER"
        ));
        sessionRepository.saveAndFlush(new AdminSession(user, UUID.randomUUID().toString(), LocalDateTime.now().minusDays(1)));

        assertThat(cleanupWorker.cleanup()).isGreaterThanOrEqualTo(1);
    }
}
