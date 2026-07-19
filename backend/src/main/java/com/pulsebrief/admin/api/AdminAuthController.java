package com.pulsebrief.admin.api;

import com.pulsebrief.admin.security.AdminIdentityService;
import com.pulsebrief.admin.security.AdminPrincipal;
import com.pulsebrief.admin.service.AdminAuthService;
import com.pulsebrief.common.api.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/auth")
@Tag(name = "Admin Auth")
public class AdminAuthController {
    private final AdminAuthService authService;
    private final AdminIdentityService identityService;

    public AdminAuthController(AdminAuthService authService, AdminIdentityService identityService) {
        this.authService = authService;
        this.identityService = identityService;
    }

    @PostMapping("/login")
    public ApiResponse<AdminLoginResponse> login(@RequestBody AdminLoginRequest request) {
        return ApiResponse.ok(authService.login(
                request == null ? null : request.username(),
                request == null ? null : request.password()
        ));
    }

    @GetMapping("/me")
    public ApiResponse<AdminProfileResponse> me() {
        AdminPrincipal principal = identityService.current();
        return ApiResponse.ok(new AdminProfileResponse(
                principal.userId(),
                principal.username(),
                principal.displayName(),
                principal.role(),
                principal.mustChangePassword()
        ));
    }

    @PostMapping("/password")
    public ApiResponse<Boolean> changePassword(@RequestBody AdminPasswordChangeRequest request) {
        authService.changePassword(
                identityService.current(),
                request == null ? null : request.currentPassword(),
                request == null ? null : request.newPassword()
        );
        return ApiResponse.ok(true);
    }

    @PostMapping("/logout")
    public ApiResponse<Boolean> logout(HttpServletRequest request) {
        authService.logout(bearerToken(request));
        return ApiResponse.ok(true);
    }

    private String bearerToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        return authorization != null && authorization.startsWith("Bearer ")
                ? authorization.substring(7).trim()
                : null;
    }
}
