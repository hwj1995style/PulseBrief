package com.pulsebrief.admin.api;

public record AdminUserCreateRequest(String username, String displayName, String role, String temporaryPassword) {
}
