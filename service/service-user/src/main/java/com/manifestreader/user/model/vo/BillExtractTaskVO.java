package com.manifestreader.user.model.vo;

import java.time.LocalDateTime;

public record BillExtractTaskVO(
        String taskNo,
        String status,
        String fileName,
        String message,
        String errorMessage,
        BillExtractResultVO result,
        LocalDateTime createdAt,
        LocalDateTime startedAt,
        LocalDateTime finishedAt
) {
}
