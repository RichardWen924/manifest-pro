package com.manifestreader.user.messaging;

public record TemplateExportTaskMessage(
        String taskNo,
        Long sourceFileId,
        Long templateId,
        String outputFormat,
        Long companyId,
        Long userId,
        String traceId
) {
}
