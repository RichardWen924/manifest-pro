package com.manifest.auth.security;

import java.util.Collection;

public interface PermissionChecker {

    boolean hasPermission(Long userId, String permissionCode);

    boolean hasAnyPermission(Long userId, Collection<String> permissionCodes);
}
