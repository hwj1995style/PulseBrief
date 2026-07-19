package com.pulsebrief.admin.api;

public record AdminIngestionScheduleRequest(Boolean enabled, Integer intervalMinutes) {
}
