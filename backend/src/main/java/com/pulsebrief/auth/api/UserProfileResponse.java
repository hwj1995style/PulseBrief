package com.pulsebrief.auth.api;

public record UserProfileResponse(
        Long id,
        String nickname,
        String avatarUrl,
        String bio
) {
}
