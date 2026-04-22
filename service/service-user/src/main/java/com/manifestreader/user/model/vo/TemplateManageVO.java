package com.manifestreader.user.model.vo;

import java.time.LocalDateTime;

public record TemplateManageVO(
        Long id,
        String templateCode,
        String templateName,
        String templateType,
        Integer status,
        Long currentVersionId,
        Integer versionNo,
        Long fileAssetId,
        String fileName,
        String storageType,
        String objectKey,
        String contentFormat,
        Integer fieldCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
