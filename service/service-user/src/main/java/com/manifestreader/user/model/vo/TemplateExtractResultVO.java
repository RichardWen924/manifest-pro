package com.manifestreader.user.model.vo;

import java.util.List;

public record TemplateExtractResultVO(
        String extractId,
        String fileName,
        Integer fieldCount,
        String templateStatus,
        String templateMessage,
        String blankTemplateDownloadUrl,
        String templatePreviewUrl,
        String templatePreviewContentType,
        List<TemplateFieldMappingVO> mappings,
        String rawText
) {
}
