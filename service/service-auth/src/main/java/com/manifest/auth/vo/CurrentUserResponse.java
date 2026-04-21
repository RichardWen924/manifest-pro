package com.manifest.auth.vo;

import java.util.List;

public record CurrentUserResponse(
        Long userId,
        Long companyId,
        String username,
        String nickname,
        List<String> roleCodes,
        List<String> permissionCodes
) {
}
