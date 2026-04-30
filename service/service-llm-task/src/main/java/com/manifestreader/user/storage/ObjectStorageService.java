package com.manifestreader.user.storage;

import java.nio.file.Path;

public interface ObjectStorageService {

    StoredObject put(String objectKey, Path file, String contentType);

    Path downloadToTemp(String bucketName, String objectKey, String fileName);
}
