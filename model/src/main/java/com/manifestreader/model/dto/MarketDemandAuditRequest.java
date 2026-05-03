package com.manifestreader.model.dto;

import jakarta.validation.constraints.NotBlank;

public record MarketDemandAuditRequest(
        @NotBlank String auditStatus,
        String auditRemark
) {
}
