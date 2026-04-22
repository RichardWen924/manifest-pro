package com.manifestreader.user.storage;

import com.manifestreader.common.exception.BusinessException;
import com.manifestreader.common.exception.ErrorCode;
import com.manifestreader.user.properties.ObjectStorageProperties;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.errors.MinioException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class MinioObjectStorageService implements ObjectStorageService {

    private static final Logger log = LoggerFactory.getLogger(MinioObjectStorageService.class);

    private final ObjectStorageProperties properties;
    private final MinioClient minioClient;

    public MinioObjectStorageService(ObjectStorageProperties properties) {
        this.properties = properties;
        this.minioClient = MinioClient.builder()
                .endpoint(properties.getEndpoint())
                .credentials(properties.getAccessKey(), properties.getSecretKey())
                .build();
    }

    @Override
    public StoredObject put(String objectKey, Path file, String contentType) {
        ensureBucket();
        try (InputStream inputStream = Files.newInputStream(file)) {
            long size = Files.size(file);
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(properties.getBucket())
                    .object(objectKey)
                    .stream(inputStream, size, -1)
                    .contentType(StringUtils.hasText(contentType) ? contentType : "application/octet-stream")
                    .build());
            return new StoredObject("MINIO", properties.getBucket(), objectKey, size);
        } catch (Exception ex) {
            log.warn("MinIO upload failed, bucket={}, objectKey={}", properties.getBucket(), objectKey, ex);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR.getCode(), "MinIO 文件上传失败，请检查 MinIO 服务和账号配置");
        }
    }

    @Override
    public Path downloadToTemp(String bucketName, String objectKey, String fileName) {
        String bucket = StringUtils.hasText(bucketName) ? bucketName : properties.getBucket();
        try {
            Path tempDir = Files.createTempDirectory("manifest-minio-");
            Path output = tempDir.resolve(StringUtils.hasText(fileName) ? fileName : Path.of(objectKey).getFileName().toString());
            try (InputStream inputStream = minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .build())) {
                Files.copy(inputStream, output);
            }
            return output;
        } catch (Exception ex) {
            log.warn("MinIO download failed, bucket={}, objectKey={}", bucket, objectKey, ex);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR.getCode(), "MinIO 文件下载失败，模板文件不可读取");
        }
    }

    private void ensureBucket() {
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder()
                    .bucket(properties.getBucket())
                    .build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder()
                        .bucket(properties.getBucket())
                        .build());
            }
        } catch (MinioException ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR.getCode(), "MinIO bucket 初始化失败：" + ex.getMessage());
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR.getCode(), "MinIO bucket 初始化失败");
        }
    }
}
