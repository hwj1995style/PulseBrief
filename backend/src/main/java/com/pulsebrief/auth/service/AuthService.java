package com.pulsebrief.auth.service;

import com.pulsebrief.auth.api.AuthResponse;
import com.pulsebrief.auth.api.LoginRequest;

public interface AuthService {
    AuthResponse login(LoginRequest request);

    AuthResponse guest();
}
