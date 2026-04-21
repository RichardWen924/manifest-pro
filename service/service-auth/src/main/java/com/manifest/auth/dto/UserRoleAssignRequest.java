package com.manifest.auth.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record UserRoleAssignRequest(@NotEmpty List<Long> roleIds) {
}
