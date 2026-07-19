package com.pulsebrief.admin.api;

public record AdminUserUpdateRequest(String displayName, String role, String status) {
}
