package com.manifest.auth.token;

import java.time.Instant;
import java.util.List;

public record TokenPrincipal(
        Long userId,
        Long companyId,
        String username,
        List<String> roleCodes,
        List<String> permissionCodes,
        String jti,
        Instant issuedAt,
        Instant expiresAt
) {
}
