package com.manifestreader.user.messaging;

public record BillParseTaskMessage(
        String taskNo,
        Long sourceFileId,
        String fileName,
        String contentType,
        String fileHash,
        Long companyId,
        Long userId,
        String traceId
) {
}
