package com.pulsebrief.auth.api;

public record AuthResponse(
        String accessToken,
        String tokenType,
        Long expiresIn,
        UserProfileResponse user
) {
}
