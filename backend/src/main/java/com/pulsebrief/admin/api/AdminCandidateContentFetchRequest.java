package com.pulsebrief.admin.api;

public record AdminCandidateContentFetchRequest(String mode) {
    public String modeOrDefault() {
        return mode == null || mode.isBlank() ? "SNIPPET" : mode.trim().toUpperCase();
    }
}
