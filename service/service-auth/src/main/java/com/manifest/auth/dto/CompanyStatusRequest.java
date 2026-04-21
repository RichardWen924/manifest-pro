package com.manifest.auth.dto;

import jakarta.validation.constraints.NotNull;

public record CompanyStatusRequest(@NotNull Integer status) {
}
