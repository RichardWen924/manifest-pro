package com.manifestreader.user.model.vo;

import java.util.List;

public record ProfileVO(
        Long userId,
        Long companyId,
        String username,
        String nickname,
        String mobile,
        String email,
        List<String> roleCodes,
        List<String> permissionCodes
) {
}
