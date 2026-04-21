package com.manifest.auth.dto;

public record RoleUpdateRequest(
        String roleName,
        String roleScope,
        Integer status,
        String remark
) {
}
