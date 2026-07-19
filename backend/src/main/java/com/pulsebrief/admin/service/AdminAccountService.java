package com.pulsebrief.admin.service;

import com.pulsebrief.admin.api.AdminUserCreateRequest;
import com.pulsebrief.admin.api.AdminUserPasswordResetRequest;
import com.pulsebrief.admin.api.AdminUserResponse;
import com.pulsebrief.admin.api.AdminUserUpdateRequest;
import com.pulsebrief.admin.domain.AdminUser;
import com.pulsebrief.admin.repository.AdminSessionRepository;
import com.pulsebrief.admin.repository.AdminUserRepository;
import com.pulsebrief.admin.security.AdminPrincipal;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminAccountService {
    private static final List<String> ROLES = List.of("VIEWER", "EDITOR", "ADMIN");
    private static final List<String> STATUSES = List.of("ACTIVE", "DISABLED");
    private final AdminUserRepository userRepository;
    private final AdminSessionRepository sessionRepository;
    private final AdminAuthService authService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(12);

    public AdminAccountService(
            AdminUserRepository userRepository,
            AdminSessionRepository sessionRepository,
            AdminAuthService authService
    ) {
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
        this.authService = authService;
    }

    @Transactional(readOnly = true)
    public List<AdminUserResponse> list() {
        return userRepository.findAll(Sort.by(Sort.Direction.ASC, "username")).stream().map(this::response).toList();
    }

    @Transactional
    public AdminUserResponse create(AdminUserCreateRequest request) {
        String username = normalizeUsername(request == null ? null : request.username());
        if (!username.matches("[a-z0-9][a-z0-9._-]{2,63}")) {
            throw badRequest("Admin username must be 3 to 64 safe characters");
        }
        if (userRepository.findByUsernameIgnoreCase(username).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Admin username already exists");
        }
        String displayName = required(request.displayName(), "Admin display name is required", 128);
        String role = role(request.role());
        authService.validatePassword(request.temporaryPassword(), username);
        String passwordHash = passwordEncoder.encode(request.temporaryPassword());
        AdminUser user = new AdminUser(username, passwordHash, displayName, role);
        user.replacePassword(passwordHash, true);
        return response(userRepository.save(user));
    }

    @Transactional
    public AdminUserResponse update(Long id, AdminUserUpdateRequest request, AdminPrincipal operator) {
        AdminUser user = find(id);
        String displayName = required(request == null ? null : request.displayName(),
                "Admin display name is required", 128);
        String role = role(request.role());
        String status = status(request.status());
        boolean removesAdmin = "ADMIN".equals(user.getRoleCode()) && "ACTIVE".equals(user.getUserStatus())
                && (!"ADMIN".equals(role) || !"ACTIVE".equals(status));
        if (removesAdmin && userRepository.countByRoleCodeAndUserStatus("ADMIN", "ACTIVE") <= 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "At least one active ADMIN account is required");
        }
        if (user.getId().equals(operator.userId()) && (!"ADMIN".equals(role) || !"ACTIVE".equals(status))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Current administrator cannot demote or disable itself");
        }
        user.updateProfile(displayName, role, status);
        if (!"ACTIVE".equals(status)) {
            sessionRepository.deleteByAdminUserId(user.getId());
        }
        return response(user);
    }

    @Transactional
    public AdminUserResponse resetPassword(Long id, AdminUserPasswordResetRequest request) {
        AdminUser user = find(id);
        authService.validatePassword(request == null ? null : request.temporaryPassword(), user.getUsername());
        user.replacePassword(passwordEncoder.encode(request.temporaryPassword()), true);
        sessionRepository.deleteByAdminUserId(user.getId());
        return response(user);
    }

    private AdminUser find(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Admin user not found"));
    }

    private AdminUserResponse response(AdminUser user) {
        return new AdminUserResponse(user.getId(), user.getUsername(), user.getDisplayName(), user.getRoleCode(),
                user.getUserStatus(), user.isMustChangePassword(), user.getPasswordChangedAt(), user.getLastLoginAt(),
                user.getCreatedAt());
    }

    private String normalizeUsername(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String role(String value) {
        String normalized = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        if (!ROLES.contains(normalized)) throw badRequest("Admin role must be VIEWER, EDITOR, or ADMIN");
        return normalized;
    }

    private String status(String value) {
        String normalized = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        if (!STATUSES.contains(normalized)) throw badRequest("Admin status must be ACTIVE or DISABLED");
        return normalized;
    }

    private String required(String value, String message, int maxLength) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isBlank() || normalized.length() > maxLength) throw badRequest(message);
        return normalized;
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
