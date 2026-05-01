package com.manifestreader.user.messaging;

public record TemplateSaveTaskMessage(
        String taskNo,
        Long companyId,
        Long userId,
        String traceId
) {
}
