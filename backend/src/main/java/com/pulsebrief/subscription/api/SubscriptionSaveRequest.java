package com.pulsebrief.subscription.api;

import java.util.List;

public record SubscriptionSaveRequest(
        List<String> categoryCodes,
        PushPreferencesResponse pushPreferences
) {
}
