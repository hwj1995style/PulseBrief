package com.pulsebrief.admin.repository;

import com.pulsebrief.admin.domain.AdminSession;
import java.util.Optional;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminSessionRepository extends JpaRepository<AdminSession, Long> {
    Optional<AdminSession> findByTokenHash(String tokenHash);

    long deleteByAdminUserId(Long adminUserId);

    @Modifying
    @Query("delete from AdminSession s where s.expiresAt < :now or (s.revokedAt is not null and s.revokedAt < :revokedBefore)")
    int deleteExpiredOrOldRevoked(
            @Param("now") LocalDateTime now,
            @Param("revokedBefore") LocalDateTime revokedBefore
    );
}
