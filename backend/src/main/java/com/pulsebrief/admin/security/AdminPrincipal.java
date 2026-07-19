package com.pulsebrief.admin.security;

public record AdminPrincipal(
        Long userId,
        String username,
        String displayName,
        String role,
        boolean mustChangePassword
) {
    public AdminPrincipal(Long userId, String username, String displayName, String role) {
        this(userId, username, displayName, role, false);
    }
}
