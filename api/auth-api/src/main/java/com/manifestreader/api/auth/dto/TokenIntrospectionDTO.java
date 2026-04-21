package com.manifestreader.api.auth.dto;

import java.time.Instant;
import java.util.List;

public record TokenIntrospectionDTO(
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
