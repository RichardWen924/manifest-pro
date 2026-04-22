package com.manifestreader.user.model.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
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
        @NotEmpty(message = "模板字段映射不能为空")
        List<TemplateFieldMappingSaveRequest> mappings,
        String rawText
) {
}
