package com.pulsebrief.admin.service;

import com.pulsebrief.admin.api.AdminLoginResponse;
import com.pulsebrief.admin.config.AdminSecurityProperties;
import com.pulsebrief.admin.domain.AdminSession;
import com.pulsebrief.admin.domain.AdminUser;
import com.pulsebrief.admin.repository.AdminSessionRepository;
import com.pulsebrief.admin.repository.AdminUserRepository;
import com.pulsebrief.admin.security.AdminPrincipal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Service
public class AdminAuthService {
    private final AdminUserRepository userRepository;
    private final AdminSessionRepository sessionRepository;
    private final AdminSecurityProperties properties;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(12);
    private final SecureRandom secureRandom = new SecureRandom();

    public AdminAuthService(
            AdminUserRepository userRepository,
            AdminSessionRepository sessionRepository,
            AdminSecurityProperties properties
    ) {
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
        this.properties = properties;
    }

    @Transactional(noRollbackFor = ResponseStatusException.class)
    public AdminLoginResponse login(String username, String password) {
        String normalizedUsername = normalizeUsername(username);
        AdminUser user = userRepository.findByUsernameIgnoreCase(normalizedUsername)
                .orElseThrow(this::invalidCredentials);
        LocalDateTime now = LocalDateTime.now();
        if (!"ACTIVE".equals(user.getUserStatus()) || user.isLocked(now)) {
            throw invalidCredentials();
        }
        if (password == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            user.recordLoginFailure(properties.maxFailedAttempts(), properties.lockMinutes());
            throw invalidCredentials();
        }
        user.recordLoginSuccess();
        String token = newToken();
        LocalDateTime expiresAt = now.plusHours(properties.sessionHours());
        sessionRepository.save(new AdminSession(user, sha256(token), expiresAt));
        return new AdminLoginResponse(
                token,
                expiresAt,
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getRoleCode(),
                user.passwordChangeRequired(now, properties.passwordMaxAgeDays())
        );
    }

    @Transactional(readOnly = true)
    public AdminPrincipal authenticate(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return null;
        }
        return sessionRepository.findByTokenHash(sha256(rawToken.trim()))
                .filter(session -> session.isActive(LocalDateTime.now()))
                .map(AdminSession::getAdminUser)
                .map(user -> new AdminPrincipal(
                        user.getId(),
                        user.getUsername(),
                        user.getDisplayName(),
                        user.getRoleCode(),
                        user.passwordChangeRequired(LocalDateTime.now(), properties.passwordMaxAgeDays())
                ))
                .orElse(null);
    }

    @Transactional
    public void logout(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }
        sessionRepository.findByTokenHash(sha256(rawToken.trim())).ifPresent(AdminSession::revoke);
    }

    @Transactional
    public void changePassword(AdminPrincipal principal, String currentPassword, String newPassword) {
        AdminUser user = userRepository.findById(principal.userId()).orElseThrow(this::invalidCredentials);
        if (currentPassword == null || !passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw invalidCredentials();
        }
        validatePassword(newPassword, user.getUsername());
        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,
                    "New password must differ from the current password");
        }
        user.replacePassword(passwordEncoder.encode(newPassword), false);
        sessionRepository.deleteByAdminUserId(user.getId());
    }

    @Transactional
    public void bootstrapIfConfigured() {
        String username = properties.bootstrapUsername();
        String password = properties.bootstrapPassword();
        if ((username == null || username.isBlank()) && (password == null || password.isBlank())) {
            return;
        }
        if (username == null || username.isBlank() || password == null || password.length() < 12) {
            throw new IllegalStateException(
                    "Admin bootstrap requires PULSEBRIEF_ADMIN_BOOTSTRAP_USERNAME and a password of at least 12 characters");
        }
        String role = properties.bootstrapRole();
        if (!role.equals("VIEWER") && !role.equals("EDITOR") && !role.equals("ADMIN")) {
            throw new IllegalStateException("Admin bootstrap role must be VIEWER, EDITOR, or ADMIN");
        }
        String normalizedUsername = normalizeUsername(username);
        if (userRepository.findByUsernameIgnoreCase(normalizedUsername).isEmpty()) {
            userRepository.save(new AdminUser(
                    normalizedUsername,
                    passwordEncoder.encode(password),
                    properties.bootstrapDisplayName(),
                    role
            ));
        }
    }

    private String normalizeUsername(String username) {
        if (username == null || username.isBlank()) {
            return "";
        }
        return username.trim().toLowerCase(Locale.ROOT);
    }

    void validatePassword(String password, String username) {
        if (password == null || password.length() < 12 || password.length() > 128) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Admin password must contain 12 to 128 characters");
        }
        String normalizedPassword = password.toLowerCase(Locale.ROOT);
        if (username != null && normalizedPassword.contains(username.toLowerCase(Locale.ROOT))) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Admin password must not contain the username");
        }
    }

    private String newToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private ResponseStatusException invalidCredentials() {
        return new ResponseStatusException(UNAUTHORIZED, "Admin username or password is invalid");
    }
}
