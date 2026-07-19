package com.pulsebrief.admin.api;

import java.time.LocalDateTime;

public record AdminLoginResponse(
        String token,
        LocalDateTime expiresAt,
        Long userId,
        String username,
        String displayName,
        String role
) {
}
