package com.pulsebrief.admin.api;

public record AdminPasswordChangeRequest(String currentPassword, String newPassword) {
}
