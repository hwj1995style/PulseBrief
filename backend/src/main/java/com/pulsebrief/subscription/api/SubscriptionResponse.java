package com.pulsebrief.subscription.api;

import java.util.List;

public record SubscriptionResponse(
        List<String> selectedCategoryCodes,
        Integer todayMatchedCount,
        PushPreferencesResponse pushPreferences
) {
}
