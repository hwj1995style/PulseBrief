package com.pulsebrief.admin.security;

import com.pulsebrief.admin.config.AdminSecurityProperties;
import com.pulsebrief.admin.service.AdminAuthService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class AdminTokenFilter extends OncePerRequestFilter {
    private final ObjectProvider<AdminSecurityProperties> propertiesProvider;
    private final ObjectProvider<AdminAuthService> authServiceProvider;

    public AdminTokenFilter(
            ObjectProvider<AdminSecurityProperties> propertiesProvider,
            ObjectProvider<AdminAuthService> authServiceProvider
    ) {
        this.propertiesProvider = propertiesProvider;
        this.authServiceProvider = authServiceProvider;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String uri = request.getRequestURI();
        return !uri.startsWith("/api/admin/") || uri.equals("/api/admin/auth/login");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String token = bearerToken(request);
        if (token == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Admin session required");
            return;
        }
        AdminAuthService authService = authServiceProvider.getIfAvailable();
        AdminPrincipal principal = authService == null ? null : authService.authenticate(token);
        if (principal == null && isLegacyToken(token)) {
            principal = new AdminPrincipal(null, "legacy-admin", "Legacy Admin", "ADMIN", false);
        }
        if (principal == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Admin session is invalid or expired");
            return;
        }
        if (principal.mustChangePassword() && !isPasswordRotationRequest(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Admin password change required");
            return;
        }
        if (!isAuthorized(principal.role(), request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Admin role does not permit this operation");
            return;
        }
        request.setAttribute(AdminIdentityService.PRINCIPAL_ATTRIBUTE, principal);
        filterChain.doFilter(request, response);
    }

    private boolean isAuthorized(String role, HttpServletRequest request) {
        if (request.getRequestURI().startsWith("/api/admin/users")) {
            return role.equals("ADMIN");
        }
        if ("GET".equalsIgnoreCase(request.getMethod())) {
            return role.equals("VIEWER") || role.equals("EDITOR") || role.equals("ADMIN");
        }
        if (request.getRequestURI().startsWith("/api/admin/ingestion/")) {
            return role.equals("ADMIN");
        }
        return role.equals("EDITOR") || role.equals("ADMIN");
    }

    private boolean isPasswordRotationRequest(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.equals("/api/admin/auth/me")
                || uri.equals("/api/admin/auth/password")
                || uri.equals("/api/admin/auth/logout");
    }

    private boolean isLegacyToken(String token) {
        AdminSecurityProperties properties = propertiesProvider.getIfAvailable();
        if (properties == null) {
            return false;
        }
        if (!properties.legacyTokenEnabled() || properties.legacyToken() == null) {
            return false;
        }
        return MessageDigest.isEqual(
                token.getBytes(StandardCharsets.UTF_8),
                properties.legacyToken().getBytes(StandardCharsets.UTF_8)
        );
    }

    private String bearerToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        return authorization != null && authorization.startsWith("Bearer ")
                ? authorization.substring(7).trim()
                : null;
    }
}
