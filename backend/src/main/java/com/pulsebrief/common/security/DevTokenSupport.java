package com.pulsebrief.common.security;

import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

public final class DevTokenSupport {
    private static final String PREFIX = "Bearer dev-token-";

    private DevTokenSupport() {
    }

    public static Long requireUserId(String authorization) {
        if (authorization == null || !authorization.startsWith(PREFIX)) {
            throw new ResponseStatusException(UNAUTHORIZED, "Unauthorized");
        }
        try {
            return Long.parseLong(authorization.substring(PREFIX.length()));
        } catch (NumberFormatException exception) {
            throw new ResponseStatusException(UNAUTHORIZED, "Unauthorized");
        }
    }
}
