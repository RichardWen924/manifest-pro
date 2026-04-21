package com.manifest.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UserCreateRequest(
        @NotNull Long companyId,
        @NotBlank String username,
        @NotBlank String password,
        String nickname,
        String mobile,
        String email,
        Integer status
) {
}
