package com.manifestreader.user.model.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;

public record BillExtractSaveRequest(
        @NotBlank(message = "extractId 不能为空")
        String extractId,
        Long templateId,
        String sourceFileName,
        Map<String, Object> fields
) {
}
