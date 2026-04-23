package com.manifestreader.user.model.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record TemplateExtractSaveRequest(
        @NotBlank(message = "extractId 不能为空")
        String extractId,
        @NotBlank(message = "fileName 不能为空")
        String fileName,
        Boolean saveAsTemplate,
        @NotBlank(message = "templateName 不能为空")
        String templateName,
        String templateType,
        @Valid
        List<TemplateFieldMappingSaveRequest> mappings,
        String rawText
) {
}
