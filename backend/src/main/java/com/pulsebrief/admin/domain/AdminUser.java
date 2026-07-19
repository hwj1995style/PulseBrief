package com.pulsebrief.admin.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "admin_user")
public class AdminUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "role_code")
    private String roleCode;

    @Column(name = "user_status")
    private String userStatus;

    @Column(name = "failed_login_count")
    private Integer failedLoginCount;

    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    protected AdminUser() {
    }

    public AdminUser(String username, String passwordHash, String displayName, String roleCode) {
        LocalDateTime now = LocalDateTime.now();
        this.username = username;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
        this.roleCode = roleCode;
        this.userStatus = "ACTIVE";
        this.failedLoginCount = 0;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void recordLoginSuccess() {
        this.failedLoginCount = 0;
        this.lockedUntil = null;
        this.lastLoginAt = LocalDateTime.now();
        this.updatedAt = this.lastLoginAt;
    }

    public void recordLoginFailure(int maxFailedAttempts, int lockMinutes) {
        LocalDateTime now = LocalDateTime.now();
        this.failedLoginCount = (failedLoginCount == null ? 0 : failedLoginCount) + 1;
        if (this.failedLoginCount >= maxFailedAttempts) {
            this.lockedUntil = now.plusMinutes(lockMinutes);
            this.failedLoginCount = 0;
        }
        this.updatedAt = now;
    }

    public boolean isLocked(LocalDateTime now) {
        return lockedUntil != null && lockedUntil.isAfter(now);
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public String getDisplayName() { return displayName; }
    public String getRoleCode() { return roleCode; }
    public String getUserStatus() { return userStatus; }
    public Integer getFailedLoginCount() { return failedLoginCount; }
    public LocalDateTime getLockedUntil() { return lockedUntil; }
}
