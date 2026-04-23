package com.manifestreader.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.manifestreader.common.constant.HeaderConstants;
import com.manifestreader.common.exception.BusinessException;
import com.manifestreader.common.exception.ErrorCode;
import com.manifestreader.model.entity.FileAssetEntity;
import com.manifestreader.user.mapper.UserFileAssetMapper;
import com.manifestreader.user.model.dto.FileUploadInitRequest;
import com.manifestreader.user.model.vo.FileAssetVO;
import com.manifestreader.user.model.vo.FileDownloadVO;
import com.manifestreader.user.service.UserFileService;
import com.manifestreader.user.storage.ObjectStorageService;
import com.manifestreader.user.storage.StoredObject;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

@Service
public class UserFileServiceImpl implements UserFileService {

    private static final Long DEFAULT_COMPANY_ID = 2L;
    private static final Long DEFAULT_USER_ID = 3L;
    private static final String STORAGE_TYPE_MINIO = "MINIO";
    private static final String DEFAULT_BIZ_TYPE = "USER_UPLOAD";

    private final UserFileAssetMapper fileAssetMapper;
    private final ObjectStorageService objectStorageService;

    public UserFileServiceImpl(UserFileAssetMapper fileAssetMapper, ObjectStorageService objectStorageService) {
        this.fileAssetMapper = fileAssetMapper;
        this.objectStorageService = objectStorageService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileAssetVO initUpload(FileUploadInitRequest request) {
        String originalName = safeFileName(request.originalName());
        FileAssetEntity entity = new FileAssetEntity();
        entity.setCompanyId(currentCompanyId());
        entity.setBizType(normalizeBizType(request.bizType()));
        entity.setFileName(originalName);
        entity.setOriginalName(originalName);
        entity.setContentType(safeContentType(request.contentType()));
        entity.setFileSize(request.fileSize());
        entity.setStorageType(STORAGE_TYPE_MINIO);
        entity.setObjectKey(buildObjectKey(entity.getBizType(), originalName));
        entity.setStatus(0);
        entity.setCreatedBy(currentUserId());
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        entity.setDeleted(0);
        fileAssetMapper.insert(entity);
        return toVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileAssetVO upload(MultipartFile file, String bizType) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "上传文件不能为空");
        }
        String originalName = safeFileName(file.getOriginalFilename());
        String normalizedBizType = normalizeBizType(bizType);
        Path tempFile = null;
        try {
            tempFile = Files.createTempFile("manifest-upload-", "-" + originalName);
            file.transferTo(tempFile);
            String objectKey = buildObjectKey(normalizedBizType, originalName);
            StoredObject storedObject = objectStorageService.put(objectKey, tempFile, safeContentType(file.getContentType()));

            FileAssetEntity entity = new FileAssetEntity();
            entity.setCompanyId(currentCompanyId());
            entity.setBizType(normalizedBizType);
            entity.setFileName(originalName);
            entity.setOriginalName(originalName);
            entity.setContentType(safeContentType(file.getContentType()));
            entity.setFileSize(storedObject.size());
            entity.setStorageType(storedObject.storageType());
            entity.setBucketName(storedObject.bucketName());
            entity.setObjectKey(storedObject.objectKey());
            entity.setFileHash(hashFile(tempFile));
            entity.setStatus(1);
            entity.setCreatedBy(currentUserId());
            entity.setCreatedAt(LocalDateTime.now());
            entity.setUpdatedAt(LocalDateTime.now());
            entity.setDeleted(0);
            fileAssetMapper.insert(entity);
            return toVO(entity);
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR.getCode(), "读取上传文件失败");
        } finally {
            deleteQuietly(tempFile);
        }
    }

    @Override
    public FileAssetVO detail(Long id) {
        return toVO(findActiveFile(id));
    }

    @Override
    public FileDownloadVO download(Long id) {
        FileAssetEntity entity = findActiveFile(id);
        if (!STORAGE_TYPE_MINIO.equalsIgnoreCase(entity.getStorageType())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "当前文件不支持从对象存储下载");
        }
        Path file = objectStorageService.downloadToTemp(entity.getBucketName(), entity.getObjectKey(), entity.getFileName());
        return new FileDownloadVO(entity.getFileName(), safeContentType(entity.getContentType()), file);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        FileAssetEntity entity = findActiveFile(id);
        fileAssetMapper.update(null, new LambdaUpdateWrapper<FileAssetEntity>()
                .eq(FileAssetEntity::getId, entity.getId())
                .set(FileAssetEntity::getDeleted, 1)
                .set(FileAssetEntity::getStatus, 0)
                .set(FileAssetEntity::getUpdatedAt, LocalDateTime.now()));
    }

    private FileAssetEntity findActiveFile(Long id) {
        if (id == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "文件 ID 不能为空");
        }
        FileAssetEntity entity = fileAssetMapper.selectOne(new LambdaQueryWrapper<FileAssetEntity>()
                .eq(FileAssetEntity::getId, id)
                .eq(FileAssetEntity::getDeleted, 0)
                .and(wrapper -> wrapper.eq(FileAssetEntity::getCompanyId, currentCompanyId()).or().isNull(FileAssetEntity::getCompanyId))
                .last("LIMIT 1"));
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "文件不存在或已删除");
        }
        return entity;
    }

    private FileAssetVO toVO(FileAssetEntity entity) {
        return new FileAssetVO(
                entity.getId(),
                entity.getOriginalName(),
                entity.getContentType(),
                entity.getFileSize(),
                entity.getStorageType(),
                entity.getObjectKey()
        );
    }

    private String buildObjectKey(String bizType, String originalName) {
        LocalDate today = LocalDate.now();
        return limitText("files/" + currentCompanyId() + "/" + bizType.toLowerCase(Locale.ROOT) + "/"
                + today + "/" + UUID.randomUUID() + "/" + originalName, 240);
    }

    private String normalizeBizType(String bizType) {
        String value = StringUtils.hasText(bizType) ? bizType.trim() : DEFAULT_BIZ_TYPE;
        return value.replaceAll("[^A-Za-z0-9_\\-]", "_").toUpperCase(Locale.ROOT);
    }

    private String safeFileName(String fileName) {
        String value = StringUtils.hasText(fileName) ? Path.of(fileName).getFileName().toString() : "upload.bin";
        return limitText(value.replaceAll("[\\\\/:*?\"<>|]", "_"), 120);
    }

    private String safeContentType(String contentType) {
        return StringUtils.hasText(contentType) ? contentType : MediaType.APPLICATION_OCTET_STREAM_VALUE;
    }

    private String hashFile(Path file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream inputStream = Files.newInputStream(file);
                 DigestInputStream digestInputStream = new DigestInputStream(inputStream, digest)) {
                byte[] buffer = new byte[8192];
                while (digestInputStream.read(buffer) != -1) {
                    // Drain stream to update digest without loading large files into memory.
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR.getCode(), "读取上传文件失败");
        } catch (NoSuchAlgorithmException ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR.getCode(), "文件指纹算法不可用");
        }
    }

    private Long currentCompanyId() {
        return readLongHeader(HeaderConstants.COMPANY_ID, DEFAULT_COMPANY_ID);
    }

    private Long currentUserId() {
        return readLongHeader(HeaderConstants.USER_ID, DEFAULT_USER_ID);
    }

    private Long readLongHeader(String headerName, Long fallback) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return fallback;
        }
        String value = attributes.getRequest().getHeader(headerName);
        if (!StringUtils.hasText(value)) {
            return fallback;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private void deleteQuietly(Path file) {
        if (file == null) {
            return;
        }
        try {
            Files.deleteIfExists(file);
        } catch (IOException ignored) {
            // Temporary files are best-effort cleanup only.
        }
    }

    private String limitText(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
