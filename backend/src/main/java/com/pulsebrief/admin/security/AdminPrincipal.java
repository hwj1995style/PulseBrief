package com.pulsebrief.admin.security;

public record AdminPrincipal(Long userId, String username, String displayName, String role) {
}
