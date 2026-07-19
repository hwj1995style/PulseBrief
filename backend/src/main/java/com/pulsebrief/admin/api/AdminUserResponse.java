package com.pulsebrief.admin.api;

import java.time.LocalDateTime;

public record AdminUserResponse(
        Long id,
        String username,
        String displayName,
        String role,
        String status,
        boolean mustChangePassword,
        LocalDateTime passwordChangedAt,
        LocalDateTime lastLoginAt,
        LocalDateTime createdAt
) {
}
