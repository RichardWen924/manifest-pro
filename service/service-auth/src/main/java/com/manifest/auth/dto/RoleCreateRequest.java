package com.manifest.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record RoleCreateRequest(
        @NotBlank String roleCode,
        @NotBlank String roleName,
        String roleScope,
        Integer status,
        String remark
) {
}
