package com.pulsebrief.admin.api;

import com.pulsebrief.admin.security.AdminIdentityService;
import com.pulsebrief.admin.service.AdminAccountService;
import com.pulsebrief.common.api.ApiResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {
    private final AdminAccountService accountService;
    private final AdminIdentityService identityService;

    public AdminUserController(AdminAccountService accountService, AdminIdentityService identityService) {
        this.accountService = accountService;
        this.identityService = identityService;
    }

    @GetMapping
    public ApiResponse<List<AdminUserResponse>> list() {
        return ApiResponse.ok(accountService.list());
    }

    @PostMapping
    public ApiResponse<AdminUserResponse> create(@RequestBody AdminUserCreateRequest request) {
        return ApiResponse.ok(accountService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<AdminUserResponse> update(
            @PathVariable Long id,
            @RequestBody AdminUserUpdateRequest request
    ) {
        return ApiResponse.ok(accountService.update(id, request, identityService.current()));
    }

    @PostMapping("/{id}/password-reset")
    public ApiResponse<AdminUserResponse> resetPassword(
            @PathVariable Long id,
            @RequestBody AdminUserPasswordResetRequest request
    ) {
        return ApiResponse.ok(accountService.resetPassword(id, request));
    }
}
