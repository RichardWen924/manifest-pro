package com.manifestreader.user.model.dto;

import jakarta.validation.constraints.NotBlank;

public record FileUploadInitRequest(
        @NotBlank String originalName,
        String contentType,
        Long fileSize,
        String bizType
) {
}
