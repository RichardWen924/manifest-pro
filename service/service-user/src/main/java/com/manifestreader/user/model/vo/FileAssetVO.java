package com.manifestreader.user.model.vo;

public record FileAssetVO(
        Long id,
        String originalName,
        String contentType,
        Long fileSize,
        String storageType,
        String objectKey
) {
}
