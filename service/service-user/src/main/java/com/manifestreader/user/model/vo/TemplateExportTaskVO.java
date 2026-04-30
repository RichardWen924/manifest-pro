package com.manifestreader.user.model.vo;

import java.time.LocalDateTime;

public record TemplateExportTaskVO(
        String taskNo,
        String status,
        Long templateId,
        String outputFormat,
        String errorMessage,
        TemplateExportResultVO result,
        LocalDateTime createdAt,
        LocalDateTime startedAt,
        LocalDateTime finishedAt
) {
}
