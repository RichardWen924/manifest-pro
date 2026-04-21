package com.manifest.auth.vo;

import java.time.Instant;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        Instant expiresAt
) {
}
