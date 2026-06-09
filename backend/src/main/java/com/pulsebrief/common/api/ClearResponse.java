package com.pulsebrief.common.api;

public record ClearResponse(Boolean cleared) {
    public static ClearResponse ok() {
        return new ClearResponse(true);
    }
}
