package com.manifest.auth.vo;

import java.util.List;

public record PermissionTreeVO(
        Long id,
        Long parentId,
        String permissionCode,
        String permissionName,
        String resourceType,
        List<PermissionTreeVO> children
) {
}
