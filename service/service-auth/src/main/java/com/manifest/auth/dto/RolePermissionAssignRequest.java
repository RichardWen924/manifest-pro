package com.manifest.auth.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record RolePermissionAssignRequest(@NotEmpty List<Long> permissionIds) {
}
