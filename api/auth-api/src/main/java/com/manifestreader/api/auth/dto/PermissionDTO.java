package com.manifestreader.api.auth.dto;

public record PermissionDTO(
        Long id,
        String permissionCode,
        String permissionName,
        String resourceType
) {
}
