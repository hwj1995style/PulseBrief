package com.pulsebrief.admin.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class AdminTokenFilter extends OncePerRequestFilter {
    private final String adminToken;

    public AdminTokenFilter(@Value("${pulsebrief.admin.token:dev-admin-token}") String adminToken) {
        this.adminToken = adminToken;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        return !request.getRequestURI().startsWith("/api/admin/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || authorization.isBlank()) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Admin token required");
            return;
        }
        if (!authorization.equals("Bearer " + adminToken)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Admin token invalid");
            return;
        }
        filterChain.doFilter(request, response);
    }
}
