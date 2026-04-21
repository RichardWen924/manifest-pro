package com.manifest.auth.vo;

import java.time.Instant;
import java.util.List;

public record TokenIntrospectionResponse(
        boolean active,
        Long userId,
        Long companyId,
        String username,
        List<String> roleCodes,
        List<String> permissionCodes,
        String jti,
        Instant expiresAt
) {
}
