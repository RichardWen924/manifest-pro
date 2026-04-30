package com.manifestreader.user.storage;

import com.manifestreader.common.exception.BusinessException;
import com.manifestreader.common.exception.ErrorCode;
import com.manifestreader.user.properties.ObjectStorageProperties;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@ConditionalOnProperty(prefix = "manifest.storage", name = "type", havingValue = "local")
public class LocalObjectStorageService implements ObjectStorageService {

    private final ObjectStorageProperties properties;

    public LocalObjectStorageService(ObjectStorageProperties properties) {
        this.properties = properties;
    }

    @Override
    public StoredObject put(String objectKey, Path file, String contentType) {
        try {
            Path root = Path.of(properties.getLocalBasePath());
            Path destination = root.resolve(objectKey);
            Files.createDirectories(destination.getParent());
            Files.copy(file, destination, StandardCopyOption.REPLACE_EXISTING);
            return new StoredObject("LOCAL", "local", objectKey, Files.size(destination));
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR.getCode(), "本地对象存储写入失败");
        }
    }

    @Override
    public Path downloadToTemp(String bucketName, String objectKey, String fileName) {
        try {
            Path root = Path.of(properties.getLocalBasePath());
            Path source = root.resolve(objectKey);
            if (!Files.exists(source)) {
                throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "对象存储文件不存在");
            }
            Path tempDir = Files.createTempDirectory("manifest-local-storage-");
            Path output = tempDir.resolve(StringUtils.hasText(fileName) ? fileName : source.getFileName().toString());
            Files.copy(source, output, StandardCopyOption.REPLACE_EXISTING);
            return output;
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR.getCode(), "本地对象存储读取失败");
        }
    }
}
