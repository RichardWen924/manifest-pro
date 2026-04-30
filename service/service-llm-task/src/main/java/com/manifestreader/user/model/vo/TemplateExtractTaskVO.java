package com.manifestreader.user.model.vo;

import java.time.LocalDateTime;

public record TemplateExtractTaskVO(
        String taskNo,
        String status,
        String fileName,
        String errorMessage,
        TemplateExtractResultVO result,
        LocalDateTime createdAt,
        LocalDateTime startedAt,
        LocalDateTime finishedAt
) {
}
