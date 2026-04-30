package com.manifestreader.user.model.dto;

import jakarta.validation.constraints.NotNull;

public record TemplateStatusUpdateRequest(
        @NotNull(message = "status 不能为空")
        Integer status
) {
}
