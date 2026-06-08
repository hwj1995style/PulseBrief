package com.pulsebrief.auth.api;

import com.pulsebrief.auth.service.AuthService;
import com.pulsebrief.common.api.ApiResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@RequestBody LoginRequest request) {
        if (!Boolean.TRUE.equals(request.agreementAccepted())) {
            throw new ResponseStatusException(BAD_REQUEST, "Agreement must be accepted");
        }
        return ApiResponse.ok(authService.login(request));
    }

    @PostMapping("/guest")
    public ApiResponse<AuthResponse> guest() {
        return ApiResponse.ok(authService.guest());
    }
}
