package com.manifestreader.user.model.vo;

public record TemplateFieldMappingVO(
        String originalText,
        String placeholderKey,
        String dataType,
        String description
) {
}
