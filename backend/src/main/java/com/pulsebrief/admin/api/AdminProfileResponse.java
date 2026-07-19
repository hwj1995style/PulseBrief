package com.pulsebrief.admin.api;

public record AdminProfileResponse(Long userId, String username, String displayName, String role) {
}
