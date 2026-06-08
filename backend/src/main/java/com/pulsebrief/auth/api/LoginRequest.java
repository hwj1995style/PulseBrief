package com.pulsebrief.auth.api;

public record LoginRequest(
        String account,
        String verificationCode,
        Boolean agreementAccepted
) {
}
