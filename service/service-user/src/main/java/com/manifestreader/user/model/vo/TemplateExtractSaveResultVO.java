package com.manifestreader.user.model.vo;

public record TemplateExtractSaveResultVO(
        Long templateId,
        Long templateVersionId,
        Integer fieldCount,
        Boolean templateSaved,
        String message
) {
}
