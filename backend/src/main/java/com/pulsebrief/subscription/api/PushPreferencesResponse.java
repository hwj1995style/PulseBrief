package com.pulsebrief.subscription.api;

public record PushPreferencesResponse(
        Boolean morningDigest,
        Boolean eveningReview,
        Boolean breakingNews,
        Boolean investmentView
) {
}
