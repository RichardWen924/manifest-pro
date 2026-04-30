package com.manifestreader.user.storage;

public record StoredObject(
        String storageType,
        String bucketName,
        String objectKey,
        long size
) {
}
