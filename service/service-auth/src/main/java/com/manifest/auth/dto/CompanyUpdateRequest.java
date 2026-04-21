package com.manifest.auth.dto;

public record CompanyUpdateRequest(
        String companyName,
        String companyAbbr,
        Integer status,
        Integer vipStatus,
        String packageType,
        String remark
) {
}
