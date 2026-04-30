package com.manifestreader.user.model.dto;

public record TemplateFieldMappingSaveRequest(
        String originalText,
        @jakarta.validation.constraints.NotBlank(message = "placeholderKey 不能为空")
        String placeholderKey,
        String dataType,
        String description,
        Integer sortNo
) {
}
