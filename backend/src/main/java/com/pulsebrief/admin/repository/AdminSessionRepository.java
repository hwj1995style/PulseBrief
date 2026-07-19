package com.pulsebrief.admin.repository;

import com.pulsebrief.admin.domain.AdminSession;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminSessionRepository extends JpaRepository<AdminSession, Long> {
    Optional<AdminSession> findByTokenHash(String tokenHash);
}
