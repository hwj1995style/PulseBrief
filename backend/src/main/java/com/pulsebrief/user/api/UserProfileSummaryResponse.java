package com.pulsebrief.user.api;

public record UserProfileSummaryResponse(
        Long id,
        String nickname,
        String avatarUrl,
        String bio,
        Integer subscriptionCount,
        Integer favoriteCount,
        Integer readCount,
        Integer playCount
) {
}
