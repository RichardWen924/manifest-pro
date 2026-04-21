package com.manifest.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record TokenIntrospectionRequest(@NotBlank String token) {
}
