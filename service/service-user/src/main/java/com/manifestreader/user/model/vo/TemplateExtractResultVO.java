package com.manifestreader.user.model.vo;

import java.util.List;

public record TemplateExtractResultVO(
        String fileName,
        Integer fieldCount,
        List<TemplateFieldMappingVO> mappings,
        String rawText
) {
}
