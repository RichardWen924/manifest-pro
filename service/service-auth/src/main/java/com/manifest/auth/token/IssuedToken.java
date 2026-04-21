package com.manifest.auth.token;

import java.time.Instant;

public record IssuedToken(
        String accessToken,
        String refreshToken,
        String tokenType,
        String jti,
        Instant expiresAt
) {
}
