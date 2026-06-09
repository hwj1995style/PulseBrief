package com.pulsebrief.user.api;

import com.pulsebrief.common.api.ApiResponse;
import com.pulsebrief.common.security.DevTokenSupport;
import com.pulsebrief.user.service.UserProfileService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user/profile")
public class UserProfileController {
    private final UserProfileService userProfileService;

    public UserProfileController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @GetMapping
    public ApiResponse<UserProfileSummaryResponse> profile(
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        Long userId = DevTokenSupport.requireUserId(authorization);
        return ApiResponse.ok(userProfileService.getProfile(userId));
    }
}
