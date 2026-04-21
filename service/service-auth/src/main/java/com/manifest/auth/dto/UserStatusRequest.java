package com.manifest.auth.dto;

import jakarta.validation.constraints.NotNull;

public record UserStatusRequest(@NotNull Integer status) {
}
