package com.manifest.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record CompanyCreateRequest(
        @NotBlank String companyCode,
        @NotBlank String companyName,
        @NotBlank String companyAbbr,
        Integer status,
        Integer vipStatus,
        String packageType,
        String remark
) {
}
