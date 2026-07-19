package com.pulsebrief.admin.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Column;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "admin_session")
public class AdminSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "admin_user_id")
    private AdminUser adminUser;

    @Column(name = "token_hash", length = 64)
    private String tokenHash;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "last_seen_at")
    private LocalDateTime lastSeenAt;

    protected AdminSession() {
    }

    public AdminSession(AdminUser adminUser, String tokenHash, LocalDateTime expiresAt) {
        LocalDateTime now = LocalDateTime.now();
        this.adminUser = adminUser;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.createdAt = now;
        this.lastSeenAt = now;
    }

    public void revoke() {
        this.revokedAt = LocalDateTime.now();
    }

    public boolean isActive(LocalDateTime now) {
        return revokedAt == null
                && expiresAt.isAfter(now)
                && "ACTIVE".equals(adminUser.getUserStatus());
    }

    public AdminUser getAdminUser() { return adminUser; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
}
