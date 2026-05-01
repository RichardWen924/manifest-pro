package com.manifestreader.user.model.vo;

import java.time.LocalDateTime;

public record TemplateSaveTaskVO(
        String taskNo,
        String status,
        String errorMessage,
        TemplateExtractSaveResultVO result,
        LocalDateTime createdAt,
        LocalDateTime startedAt,
        LocalDateTime finishedAt
) {
}
