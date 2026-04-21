package com.manifest.auth.vo;

import java.time.LocalDateTime;

public record CompanyVO(
        Long id,
        String companyCode,
        String companyName,
        String companyAbbr,
        Integer status,
        Integer vipStatus,
        String packageType,
        LocalDateTime expireAt
) {
}
