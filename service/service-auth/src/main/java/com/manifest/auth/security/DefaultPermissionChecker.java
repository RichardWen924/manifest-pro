package com.manifest.auth.security;

import java.util.Collection;
import org.springframework.stereotype.Component;

@Component
public class DefaultPermissionChecker implements PermissionChecker {

    @Override
    public boolean hasPermission(Long userId, String permissionCode) {
        // TODO 从角色-权限关系或缓存查询 permission_code。
        return false;
    }

    @Override
    public boolean hasAnyPermission(Long userId, Collection<String> permissionCodes) {
        // TODO 后续可替换为统一 IAM/OIDC scope 检查。
        return permissionCodes != null && permissionCodes.stream().anyMatch(code -> hasPermission(userId, code));
    }
}
