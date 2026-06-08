package com.pulsebrief.auth.service;

import com.pulsebrief.auth.api.AuthResponse;
import com.pulsebrief.auth.api.LoginRequest;
import com.pulsebrief.auth.api.UserProfileResponse;
import org.springframework.stereotype.Service;

@Service
public class AuthApplicationService implements AuthService {
    @Override
    public AuthResponse login(LoginRequest request) {
        return new AuthResponse(
                "dev-token-1",
                "Bearer",
                604800L,
                new UserProfileResponse(1L, "Wenjin", "", "每天几分钟，掌握全球脉搏")
        );
    }

    @Override
    public AuthResponse guest() {
        return new AuthResponse(
                "",
                "Guest",
                0L,
                new UserProfileResponse(0L, "游客", "", "游客模式")
        );
    }
}
