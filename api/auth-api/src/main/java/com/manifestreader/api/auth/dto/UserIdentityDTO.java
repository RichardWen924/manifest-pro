package com.manifestreader.api.auth.dto;

import java.util.List;

public record UserIdentityDTO(
        Long userId,
        Long companyId,
        String username,
        String nickname,
        List<String> roleCodes,
        List<String> permissionCodes,
        Integer status
) {
}
