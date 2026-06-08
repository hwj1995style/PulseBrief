package com.pulsebrief.subscription.service;

import com.pulsebrief.subscription.api.SubscriptionResponse;
import com.pulsebrief.subscription.api.SubscriptionSaveRequest;

public interface SubscriptionService {
    SubscriptionResponse getSubscriptions(Long userId);

    SubscriptionResponse saveSubscriptions(Long userId, SubscriptionSaveRequest request);
}
