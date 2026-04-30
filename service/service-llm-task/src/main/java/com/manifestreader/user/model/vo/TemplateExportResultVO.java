package com.manifestreader.user.model.vo;

import java.util.List;
import java.util.Map;

public record TemplateExportResultVO(
        String exportId,
        Long templateId,
        String templateName,
        String outputFileName,
        String outputFormat,
        String downloadUrl,
        Map<String, Object> extractedFields,
        List<String> missingPlaceholders,
        String rawText,
        String status,
        String message
) {
}
