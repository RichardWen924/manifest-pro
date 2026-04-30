package com.manifestreader.user.model.dto;

import jakarta.validation.constraints.NotNull;

public record BillParseRequest(
        @NotNull Long fileAssetId,
        Long templateId
) {
}
