package com.pulsebrief.subscription.api;

import com.pulsebrief.common.api.ApiResponse;
import com.pulsebrief.common.security.DevTokenSupport;
import com.pulsebrief.subscription.service.SubscriptionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user/subscriptions")
public class SubscriptionController {
    private final SubscriptionService subscriptionService;

    public SubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @GetMapping
    public ApiResponse<SubscriptionResponse> subscriptions(
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        Long userId = DevTokenSupport.requireUserId(authorization);
        return ApiResponse.ok(subscriptionService.getSubscriptions(userId));
    }

    @PutMapping
    public ApiResponse<SubscriptionResponse> saveSubscriptions(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody SubscriptionSaveRequest request
    ) {
        Long userId = DevTokenSupport.requireUserId(authorization);
        return ApiResponse.ok(subscriptionService.saveSubscriptions(userId, request));
    }
}
