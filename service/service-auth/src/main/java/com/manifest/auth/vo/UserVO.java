package com.manifest.auth.vo;

import java.time.LocalDateTime;
import java.util.List;

public record UserVO(
        Long id,
        Long companyId,
        String username,
        String nickname,
        String mobile,
        String email,
        Integer status,
        List<String> roleCodes,
        LocalDateTime createdAt
) {
}
