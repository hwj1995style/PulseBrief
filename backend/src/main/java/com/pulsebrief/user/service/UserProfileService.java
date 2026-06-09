package com.pulsebrief.user.service;

import com.pulsebrief.user.api.UserProfileSummaryResponse;

public interface UserProfileService {
    UserProfileSummaryResponse getProfile(Long userId);
}
