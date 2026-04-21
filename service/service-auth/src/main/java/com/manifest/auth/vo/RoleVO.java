package com.manifest.auth.vo;

public record RoleVO(
        Long id,
        String roleCode,
        String roleName,
        String roleScope,
        Integer status
) {
}
