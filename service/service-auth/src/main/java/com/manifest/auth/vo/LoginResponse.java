package com.manifest.auth.vo;

import java.time.Instant;
import java.util.List;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        Instant expiresAt,
        Long userId,
        Long companyId,
        String username,
        List<String> roleCodes
) {
}
