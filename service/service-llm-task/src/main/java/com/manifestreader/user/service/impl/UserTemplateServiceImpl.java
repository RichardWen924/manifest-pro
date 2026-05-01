package com.manifestreader.user.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.manifestreader.common.constant.HeaderConstants;
import com.manifestreader.common.exception.BusinessException;
import com.manifestreader.common.exception.ErrorCode;
import com.manifestreader.common.result.PageResult;
import com.manifestreader.model.entity.BlParseTaskEntity;
import com.manifestreader.model.entity.FileAssetEntity;
import com.manifestreader.model.entity.TemplateEntity;
import com.manifestreader.model.entity.TemplateFieldMappingEntity;
import com.manifestreader.model.entity.TemplateVersionEntity;
import com.manifestreader.user.dify.DifyTemplateExportParser;
import com.manifestreader.user.dify.DifyTemplateMappingParser;
import com.manifestreader.user.dify.DifyWorkflowClient;
import com.manifestreader.user.mapper.UserBillParseTaskMapper;
import com.manifestreader.user.mapper.UserFileAssetMapper;
import com.manifestreader.user.mapper.UserTemplateFieldMappingMapper;
import com.manifestreader.user.mapper.UserTemplateMapper;
import com.manifestreader.user.mapper.UserTemplateVersionMapper;
import com.manifestreader.user.model.dto.TemplateExtractSaveRequest;
import com.manifestreader.user.model.dto.TemplateFieldMappingSaveRequest;
import com.manifestreader.user.model.dto.TemplatePageQuery;
import com.manifestreader.user.model.dto.TemplateStatusUpdateRequest;
import com.manifestreader.user.model.vo.BlankTemplateFile;
import com.manifestreader.user.model.vo.ExportedTemplateFile;
import com.manifestreader.user.model.vo.TemplateExtractResultVO;
import com.manifestreader.user.model.vo.TemplateExtractSaveResultVO;
import com.manifestreader.user.model.vo.TemplateExportResultVO;
import com.manifestreader.user.model.vo.TemplateFieldMappingVO;
import com.manifestreader.user.model.vo.TemplateManageVO;
import com.manifestreader.user.model.vo.TemplateOptionVO;
import com.manifestreader.user.service.UserTemplateService;
import com.manifestreader.user.storage.ObjectStorageService;
import com.manifestreader.user.storage.StoredObject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

@Service
public class UserTemplateServiceImpl implements UserTemplateService {

    private static final Logger log = LoggerFactory.getLogger(UserTemplateServiceImpl.class);
    private static final Long DEFAULT_COMPANY_ID = 2L;
    private static final Long DEFAULT_USER_ID = 3L;

    private final DifyWorkflowClient difyWorkflowClient;
    private final DifyTemplateMappingParser mappingParser;
    private final DifyTemplateExportParser exportParser;
    private final ObjectMapper objectMapper;
    private final UserTemplateMapper templateMapper;
    private final UserTemplateVersionMapper templateVersionMapper;
    private final UserTemplateFieldMappingMapper fieldMappingMapper;
    private final UserBillParseTaskMapper taskMapper;
    private final UserFileAssetMapper fileAssetMapper;
    private final ObjectStorageService objectStorageService;
    private final ConcurrentMap<String, TemplateExtractResultVO> extractResultCache = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, BlankTemplateFile> blankTemplateCache = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, ExportedTemplateFile> exportedTemplateCache = new ConcurrentHashMap<>();

    public UserTemplateServiceImpl(
            DifyWorkflowClient difyWorkflowClient,
            DifyTemplateMappingParser mappingParser,
            DifyTemplateExportParser exportParser,
            ObjectMapper objectMapper,
            UserTemplateMapper templateMapper,
            UserTemplateVersionMapper templateVersionMapper,
            UserTemplateFieldMappingMapper fieldMappingMapper,
            UserBillParseTaskMapper taskMapper,
            UserFileAssetMapper fileAssetMapper,
            ObjectStorageService objectStorageService
    ) {
        this.difyWorkflowClient = difyWorkflowClient;
        this.mappingParser = mappingParser;
        this.exportParser = exportParser;
        this.objectMapper = objectMapper;
        this.templateMapper = templateMapper;
        this.templateVersionMapper = templateVersionMapper;
        this.fieldMappingMapper = fieldMappingMapper;
        this.taskMapper = taskMapper;
        this.fileAssetMapper = fileAssetMapper;
        this.objectStorageService = objectStorageService;
    }

    @Override
    public PageResult<TemplateOptionVO> page(TemplatePageQuery query) {
        PageResult<TemplateManageVO> managePage = managePage(query);
        PageResult<TemplateOptionVO> pageResult = PageResult.empty(managePage.getCurrent(), managePage.getSize());
        pageResult.setTotal(managePage.getTotal());
        pageResult.setRecords(managePage.getRecords().stream()
                .map(item -> new TemplateOptionVO(item.id(), item.templateCode(), item.templateName(), item.status()))
                .toList());
        return pageResult;
    }

    @Override
    public TemplateOptionVO detail(Long id) {
        TemplateManageVO detail = manageDetail(id);
        return new TemplateOptionVO(detail.id(), detail.templateCode(), detail.templateName(), detail.status());
    }

    @Override
    public List<TemplateOptionVO> usableTemplates() {
        return templateMapper.selectList(new LambdaQueryWrapper<TemplateEntity>()
                        .eq(TemplateEntity::getDeleted, 0)
                        .eq(TemplateEntity::getStatus, 1)
                        .and(wrapper -> wrapper.eq(TemplateEntity::getCompanyId, currentCompanyId()).or().isNull(TemplateEntity::getCompanyId))
                        .orderByDesc(TemplateEntity::getUpdatedAt))
                .stream()
                .map(item -> new TemplateOptionVO(item.getId(), item.getTemplateCode(), item.getTemplateName(), item.getStatus()))
                .toList();
    }

    @Override
    public PageResult<TemplateManageVO> managePage(TemplatePageQuery query) {
        Page<TemplateEntity> page = templateMapper.selectPage(
                Page.of(query.pageNo(), query.pageSize()),
                templateQueryWrapper(query)
        );
        PageResult<TemplateManageVO> result = PageResult.empty(page.getCurrent(), page.getSize());
        result.setTotal(page.getTotal());
        result.setRecords(page.getRecords().stream().map(this::toManageVO).toList());
        return result;
    }

    @Override
    public TemplateManageVO manageDetail(Long id) {
        TemplateEntity entity = templateMapper.selectOne(new LambdaQueryWrapper<TemplateEntity>()
                .eq(TemplateEntity::getId, id)
                .eq(TemplateEntity::getDeleted, 0)
                .and(wrapper -> wrapper.eq(TemplateEntity::getCompanyId, currentCompanyId()).or().isNull(TemplateEntity::getCompanyId)));
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "模板不存在");
        }
        return toManageVO(entity);
    }

    @Override
    public void updateStatus(Long id, TemplateStatusUpdateRequest request) {
        TemplateEntity entity = templateMapper.selectById(id);
        if (entity == null || Integer.valueOf(1).equals(entity.getDeleted())) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "模板不存在");
        }
        entity.setStatus(request.status());
        entity.setUpdatedBy(currentUserId());
        entity.setUpdatedAt(LocalDateTime.now());
        templateMapper.updateById(entity);
    }

    @Override
    public void deleteTemplate(Long id) {
        TemplateEntity entity = templateMapper.selectById(id);
        if (entity == null || Integer.valueOf(1).equals(entity.getDeleted())) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "模板不存在");
        }
        entity.setDeleted(1);
        entity.setUpdatedBy(currentUserId());
        entity.setUpdatedAt(LocalDateTime.now());
        templateMapper.updateById(entity);
    }

    @Override
    public TemplateExtractResultVO extractTemplate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请上传模板样本文件");
        }
        String fileHash = hashFile(file);
        TemplateExtractResultVO cached = extractResultCache.get(fileHash);
        if (cached != null) {
            log.info("Template extraction cache hit, fileName={}, hash={}", file.getOriginalFilename(), fileHash);
            return cached;
        }
        synchronized (extractResultCache) {
            cached = extractResultCache.get(fileHash);
            if (cached != null) {
                log.info("Template extraction cache hit after lock, fileName={}, hash={}", file.getOriginalFilename(), fileHash);
                return cached;
            }
            TemplateExtractExecutionResult executionResult = executeTemplateExtraction(
                    file,
                    currentCompanyId(),
                    currentUserId(),
                    fileHash,
                    "/user/templates/extract/" + fileHash + "/blank-template",
                    "/user/templates/extract/" + fileHash + "/preview"
            );
            TemplateExtractResultVO result = executionResult.result();
            extractResultCache.put(fileHash, result);
            return result;
        }
    }

    TemplateExtractExecutionResult executeTemplateExtraction(
            MultipartFile file,
            Long companyId,
            Long userId,
            String extractId,
            String blankTemplateDownloadUrl,
            String previewUrl
    ) {
        log.info("Template extraction calling Dify, fileName={}, size={}", file.getOriginalFilename(), file.getSize());
        String difyResponse = difyWorkflowClient.runTemplateExtraction(file);
        DifyTemplateMappingParser.ParsedMappings parsed = mappingParser.parse(difyResponse);
        String fileName = StringUtils.hasText(file.getOriginalFilename()) ? file.getOriginalFilename() : "template-file";
        String resolvedExtractId = StringUtils.hasText(extractId) ? extractId : hashFile(file);
        BlankTemplateResult blankTemplate = generateBlankTemplateIfSupported(resolvedExtractId, fileName, file, parsed.mappings());
        BlankTemplateArtifacts artifacts = persistBlankTemplateArtifacts(resolvedExtractId, fileName, companyId, userId);
        String downloadUrl = artifacts.blankTemplateFileId() == null ? null : blankTemplateDownloadUrl;
        String resolvedPreviewUrl = artifacts.previewFileId() == null ? null : previewUrl;
        String previewContentType = artifacts.previewContentType() != null
                ? artifacts.previewContentType()
                : blankTemplate.previewContentType();
        return new TemplateExtractExecutionResult(new TemplateExtractResultVO(
                resolvedExtractId,
                fileName,
                parsed.mappings().size(),
                blankTemplate.status(),
                blankTemplate.message(),
                downloadUrl,
                resolvedPreviewUrl,
                previewContentType,
                parsed.mappings(),
                parsed.rawText()
        ), artifacts.blankTemplateFileId(), artifacts.previewFileId());
    }

    @Override
    public BlankTemplateFile getBlankTemplate(String extractId) {
        return getBlankTemplate(extractId, currentCompanyId());
    }

    private BlankTemplateFile getBlankTemplate(String extractId, Long companyId) {
        BlankTemplateFile file = blankTemplateCache.get(extractId);
        if (file == null) {
            file = loadAsyncBlankTemplate(extractId, companyId);
            if (file != null) {
                blankTemplateCache.putIfAbsent(extractId, file);
            }
        }
        if (file == null || !Files.exists(file.path())) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "空白模板不存在或尚未生成");
        }
        return file;
    }

    @Override
    public BlankTemplateFile getBlankTemplatePreview(String extractId) {
        BlankTemplateFile file = getBlankTemplate(extractId);
        if (file.previewPath() == null || !Files.exists(file.previewPath()) || !StringUtils.hasText(file.previewContentType())) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "模板预览不存在或尚未生成");
        }
        return file;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TemplateExtractSaveResultVO saveGeneratedTemplate(TemplateExtractSaveRequest request) {
        return saveGeneratedTemplate(request, currentCompanyId(), currentUserId());
    }

    @Transactional(rollbackFor = Exception.class)
    public TemplateExtractSaveResultVO saveGeneratedTemplate(TemplateExtractSaveRequest request, Long companyId, Long userId) {
        List<TemplateFieldMappingSaveRequest> mappings = resolveSaveMappings(request);
        if (mappings.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "没有可保存的字段映射，请重新提取模板字段后再保存");
        }

        boolean previewOnlyTemplate = isPreviewOnlyTemplate(request);
        FileAssetEntity fileAsset = previewOnlyTemplate
                ? createPreviewTemplateFileAsset(request, mappings, companyId, userId)
                : createMinioTemplateFileAsset(request, companyId, userId);
        TemplateEntity template = createTemplate(request, companyId, userId);
        TemplateVersionEntity version = createTemplateVersion(
                template.getId(),
                fileAsset == null ? null : fileAsset.getId(),
                mappings,
                previewOnlyTemplate ? "PREVIEW" : "DOCX",
                userId
        );
        template.setCurrentVersionId(version.getId());
        template.setUpdatedAt(LocalDateTime.now());
        template.setUpdatedBy(userId);
        templateMapper.updateById(template);
        saveFieldMappings(version.getId(), mappings);

        return new TemplateExtractSaveResultVO(
                template.getId(),
                version.getId(),
                mappings.size(),
                true,
                previewOnlyTemplate
                        ? "模板定义已保存，当前为预览模板，未绑定 DOCX 文件"
                        : "模板定义已保存，模板文件已写入 MinIO"
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TemplateExportResultVO exportWithTemplate(Long templateId, String outputFormat, MultipartFile file) {
        return executeTemplateExport(templateId, outputFormat, file, currentCompanyId(), currentUserId()).result();
    }

    TemplateExportExecutionResult executeTemplateExport(Long templateId, String outputFormat, MultipartFile file, Long companyId, Long userId) {
        if (templateId == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "请选择模板");
        }
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "请上传需要提取数据的目标文件");
        }

        ExportContext context = resolveExportContext(templateId, companyId);
        String normalizedFormat = normalizeOutputFormat(outputFormat);
        if ("PDF".equals(normalizedFormat) && !isSofficeAvailable()) {
            return new TemplateExportExecutionResult(new TemplateExportResultVO(
                    UUID.randomUUID().toString(),
                    context.template().getId(),
                    context.template().getTemplateName(),
                    null,
                    normalizedFormat,
                    null,
                    Map.of(),
                    List.of(),
                    "",
                    "FAILED",
                    "当前环境未安装 LibreOffice/soffice，暂无法将 DOCX 模板转换为 PDF；请先选择 DOCX 导出或安装转换工具。"
            ), null);
        }

        log.info("Template export calling Dify, templateId={}, fileName={}, outputFormat={}",
                templateId, file.getOriginalFilename(), normalizedFormat);
        String difyResponse = difyWorkflowClient.runTemplateExport(file);
        DifyTemplateExportParser.ParsedExportFields parsed = exportParser.parse(difyResponse);
        List<String> missing = resolveMissingPlaceholders(context.version().getId(), parsed.fields());
        RenderedTemplate rendered = renderTemplate(context, parsed.fields(), normalizedFormat);
        FileAssetEntity fileAsset = createExportFileAsset(rendered, file, context.template(), parsed.rawText(), companyId, userId);
        exportedTemplateCache.put(rendered.exportId(), new ExportedTemplateFile(rendered.fileName(), rendered.contentType(), rendered.path()));

        return new TemplateExportExecutionResult(new TemplateExportResultVO(
                rendered.exportId(),
                context.template().getId(),
                context.template().getTemplateName(),
                rendered.fileName(),
                normalizedFormat,
                "/user/templates/export/" + rendered.exportId() + "/download",
                parsed.fields(),
                missing,
                parsed.rawText(),
                "GENERATED",
                missing.isEmpty() ? "模板导出完成，文件已写入 MinIO" : "模板导出完成并写入 MinIO，但存在未返回字段，请检查缺失占位符"
        ), fileAsset.getId());
    }

    @Override
    public ExportedTemplateFile getExportedTemplate(String exportId) {
        ExportedTemplateFile file = exportedTemplateCache.get(exportId);
        if (file == null || !Files.exists(file.path())) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "导出文件不存在或已过期");
        }
        return file;
    }

    private ExportContext resolveExportContext(Long templateId, Long companyId) {
        TemplateEntity template = templateMapper.selectOne(new LambdaQueryWrapper<TemplateEntity>()
                .eq(TemplateEntity::getId, templateId)
                .eq(TemplateEntity::getDeleted, 0)
                .eq(TemplateEntity::getStatus, 1)
                .and(wrapper -> wrapper.eq(TemplateEntity::getCompanyId, companyId).or().isNull(TemplateEntity::getCompanyId)));
        if (template == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "模板不存在或未启用");
        }
        TemplateVersionEntity version = findCurrentVersion(template);
        if (version == null || version.getFileAssetId() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "模板未绑定可导出的文件版本");
        }
        FileAssetEntity asset = fileAssetMapper.selectById(version.getFileAssetId());
        if (asset == null || !StringUtils.hasText(asset.getObjectKey())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "模板文件资产不存在");
        }
        Path templatePath = resolveAssetToLocalPath(asset);
        if (!Files.exists(templatePath) || !templatePath.getFileName().toString().toLowerCase().endsWith(".docx")) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "当前模板没有可读取的 DOCX 文件，请先保存 DOCX 空白模板");
        }
        return new ExportContext(template, version, asset, templatePath);
    }

    private Path resolveAssetToLocalPath(FileAssetEntity asset) {
        if ("MINIO".equalsIgnoreCase(asset.getStorageType())) {
            return objectStorageService.downloadToTemp(asset.getBucketName(), asset.getObjectKey(), asset.getFileName());
        }
        return Path.of(asset.getObjectKey());
    }

    private List<String> resolveMissingPlaceholders(Long versionId, Map<String, Object> extractedFields) {
        Set<String> keys = extractedFields.keySet();
        return fieldMappingMapper.selectList(new LambdaQueryWrapper<TemplateFieldMappingEntity>()
                        .eq(TemplateFieldMappingEntity::getTemplateVersionId, versionId)
                        .orderByAsc(TemplateFieldMappingEntity::getSortNo))
                .stream()
                .map(TemplateFieldMappingEntity::getFieldKey)
                .filter(StringUtils::hasText)
                .filter(key -> !keys.contains(key) || extractedFields.get(key) == null)
                .toList();
    }

    private RenderedTemplate renderTemplate(ExportContext context, Map<String, Object> extractedFields, String outputFormat) {
        String exportId = UUID.randomUUID().toString();
        try {
            Path workDir = Files.createTempDirectory("manifest-export-" + exportId + "-");
            Path dataPath = workDir.resolve("fields.json");
            Path docxPath = workDir.resolve("rendered.docx");
            objectMapper.writeValue(dataPath.toFile(), extractedFields);

            Process process = new ProcessBuilder(
                    "python3",
                    resolveRenderScriptPath(),
                    context.templatePath().toString(),
                    dataPath.toString(),
                    docxPath.toString()
            ).redirectErrorStream(true).start();
            String processOutput = new String(process.getInputStream().readAllBytes());
            int exitCode = process.waitFor();
            if (exitCode != 0 || !Files.exists(docxPath)) {
                log.warn("DOCX template render failed, exitCode={}, output={}", exitCode, processOutput);
                throw new BusinessException(ErrorCode.INTERNAL_ERROR.getCode(), "DOCX 模板填充失败，请检查 python-docx 环境和模板占位符");
            }

            if ("PDF".equals(outputFormat)) {
                Path pdfPath = convertDocxToPdf(docxPath, workDir);
                return new RenderedTemplate(
                        exportId,
                        buildExportFileName(context.template().getTemplateName(), "pdf"),
                        "application/pdf",
                        pdfPath
                );
            }

            return new RenderedTemplate(
                    exportId,
                    buildExportFileName(context.template().getTemplateName(), "docx"),
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    docxPath
            );
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR.getCode(), "模板导出文件处理失败");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.INTERNAL_ERROR.getCode(), "模板导出被中断");
        }
    }

    private Path convertDocxToPdf(Path docxPath, Path workDir) throws IOException, InterruptedException {
        String soffice = resolveSofficeCommand();
        if (!StringUtils.hasText(soffice)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "当前环境未安装 LibreOffice/soffice，暂无法生成 PDF");
        }
        Process process = new ProcessBuilder(
                soffice,
                "--headless",
                "--convert-to",
                "pdf",
                "--outdir",
                workDir.toString(),
                docxPath.toString()
        ).redirectErrorStream(true).start();
        String processOutput = new String(process.getInputStream().readAllBytes());
        int exitCode = process.waitFor();
        Path pdfPath = workDir.resolve(stripExtension(docxPath.getFileName().toString()) + ".pdf");
        if (exitCode != 0 || !Files.exists(pdfPath)) {
            log.warn("DOCX to PDF conversion failed, exitCode={}, output={}", exitCode, processOutput);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR.getCode(), "PDF 转换失败，请检查 LibreOffice/soffice 配置");
        }
        return pdfPath;
    }

    private FileAssetEntity createExportFileAsset(RenderedTemplate rendered, MultipartFile sourceFile, TemplateEntity template, String rawText, Long companyId, Long userId) {
        StoredObject stored = objectStorageService.put(
                "exports/" + companyId + "/" + rendered.exportId() + "/" + rendered.fileName(),
                rendered.path(),
                rendered.contentType()
        );
        FileAssetEntity entity = new FileAssetEntity();
        entity.setCompanyId(companyId);
        entity.setBizType("TEMPLATE_EXPORT");
        entity.setFileName(limitText(rendered.fileName(), 255));
        entity.setOriginalName(limitText(safeFileName(sourceFile.getOriginalFilename()), 255));
        entity.setContentType(rendered.contentType());
        entity.setFileSize(stored.size());
        entity.setStorageType(stored.storageType());
        entity.setBucketName(stored.bucketName());
        entity.setObjectKey(limitText(stored.objectKey(), 255));
        entity.setFileHash(hashText(template.getId() + ":" + rendered.exportId() + ":" + rawText));
        entity.setStatus(1);
        entity.setCreatedBy(userId);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        entity.setDeleted(0);
        fileAssetMapper.insert(entity);
        return entity;
    }

    private String normalizeOutputFormat(String outputFormat) {
        String value = StringUtils.hasText(outputFormat) ? outputFormat.trim().toUpperCase() : "DOCX";
        if (!List.of("DOCX", "PDF").contains(value)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "导出格式仅支持 DOCX 或 PDF");
        }
        return value;
    }

    private String buildExportFileName(String templateName, String extension) {
        return limitText(stripExtension(safeText(templateName, "template-export")) + "-export-" + System.currentTimeMillis() + "." + extension, 255);
    }

    private Long fileSize(Path path) {
        try {
            return Files.size(path);
        } catch (IOException ex) {
            return 0L;
        }
    }

    private String hashText(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(text.getBytes()));
        } catch (NoSuchAlgorithmException ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR.getCode(), "文件指纹算法不可用");
        }
    }

    private List<TemplateFieldMappingSaveRequest> resolveSaveMappings(TemplateExtractSaveRequest request) {
        if (request.mappings() != null && !request.mappings().isEmpty()) {
            return request.mappings();
        }
        TemplateExtractResultVO cached = extractResultCache.get(request.extractId());
        if (cached != null && cached.mappings() != null && !cached.mappings().isEmpty()) {
            return cached.mappings().stream()
                    .map(item -> new TemplateFieldMappingSaveRequest(
                            item.originalText(),
                            item.placeholderKey(),
                            item.dataType(),
                            item.description(),
                            null
                    ))
                    .toList();
        }
        Optional<TemplateExtractTaskPayload> asyncPayload = findAsyncTemplateExtractPayload(request.extractId());
        if (asyncPayload.isEmpty() || asyncPayload.get().result().mappings() == null || asyncPayload.get().result().mappings().isEmpty()) {
            return List.of();
        }
        return asyncPayload.get().result().mappings().stream()
                .map(item -> new TemplateFieldMappingSaveRequest(
                        item.originalText(),
                        item.placeholderKey(),
                        item.dataType(),
                        item.description(),
                        null
                ))
                .toList();
    }

    private FileAssetEntity createMinioTemplateFileAsset(TemplateExtractSaveRequest request) {
        return createMinioTemplateFileAsset(request, currentCompanyId(), currentUserId());
    }

    private FileAssetEntity createMinioTemplateFileAsset(TemplateExtractSaveRequest request, Long companyId, Long userId) {
        BlankTemplateFile blankTemplateFile = getBlankTemplate(request.extractId(), companyId);
        if (blankTemplateFile == null || !Files.exists(blankTemplateFile.path())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "当前提取结果没有可保存的 DOCX 空白模板，请上传 DOCX 样本或先完成 PDF 转 DOCX 能力");
        }
        StoredObject storedObject = objectStorageService.put(
                "templates/" + companyId + "/" + safeText(request.extractId(), "manual-" + System.currentTimeMillis()) + "/" + blankTemplateFile.fileName(),
                blankTemplateFile.path(),
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        );
        FileAssetEntity entity = new FileAssetEntity();
        entity.setCompanyId(companyId);
        entity.setBizType("TEMPLATE");
        entity.setFileName(limitText(blankTemplateFile.fileName(), 255));
        entity.setOriginalName(limitText(safeFileName(request.fileName()), 255));
        entity.setContentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        entity.setFileSize(storedObject.size());
        entity.setStorageType(storedObject.storageType());
        entity.setBucketName(storedObject.bucketName());
        entity.setObjectKey(limitText(storedObject.objectKey(), 255));
        entity.setFileHash(request.extractId());
        entity.setStatus(1);
        entity.setCreatedBy(userId);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        entity.setDeleted(0);
        fileAssetMapper.insert(entity);
        return entity;
    }

    private FileAssetEntity createPreviewTemplateFileAsset(
            TemplateExtractSaveRequest request,
            List<TemplateFieldMappingSaveRequest> mappings
    ) {
        return createPreviewTemplateFileAsset(request, mappings, currentCompanyId(), currentUserId());
    }

    private FileAssetEntity createPreviewTemplateFileAsset(
            TemplateExtractSaveRequest request,
            List<TemplateFieldMappingSaveRequest> mappings,
            Long companyId,
            Long userId
    ) {
        Path tempFile = null;
        try {
            tempFile = Files.createTempFile("manifest-template-preview-", ".json");
            Map<String, Object> previewPayload = new LinkedHashMap<>();
            previewPayload.put("extractId", request.extractId());
            previewPayload.put("fileName", request.fileName());
            previewPayload.put("templateName", request.templateName());
            previewPayload.put("templateType", safeText(request.templateType(), "BILL_PREVIEW"));
            previewPayload.put("rawText", request.rawText());
            previewPayload.put("mappings", mappings);
            objectMapper.writeValue(tempFile.toFile(), previewPayload);

            String previewFileName = stripExtension(safeFileName(request.fileName())) + "-preview-template.json";
            StoredObject storedObject = objectStorageService.put(
                    "templates/" + companyId + "/" + safeText(request.extractId(), "preview-" + System.currentTimeMillis()) + "/" + previewFileName,
                    tempFile,
                    "application/json"
            );

            FileAssetEntity entity = new FileAssetEntity();
            entity.setCompanyId(companyId);
            entity.setBizType("TEMPLATE_PREVIEW");
            entity.setFileName(limitText(previewFileName, 255));
            entity.setOriginalName(limitText(safeFileName(request.fileName()), 255));
            entity.setContentType("application/json");
            entity.setFileSize(storedObject.size());
            entity.setStorageType(storedObject.storageType());
            entity.setBucketName(storedObject.bucketName());
            entity.setObjectKey(limitText(storedObject.objectKey(), 255));
            entity.setFileHash(limitText(request.extractId(), 64));
            entity.setStatus(1);
            entity.setCreatedBy(userId);
            entity.setCreatedAt(LocalDateTime.now());
            entity.setUpdatedAt(LocalDateTime.now());
            entity.setDeleted(0);
            fileAssetMapper.insert(entity);
            return entity;
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR.getCode(), "预览模板资产生成失败");
        } finally {
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException ignored) {
                    // ignore temp preview cleanup errors
                }
            }
        }
    }

    private BlankTemplateArtifacts persistBlankTemplateArtifacts(String extractId, String originalFileName, Long companyId, Long userId) {
        BlankTemplateFile blankTemplateFile = blankTemplateCache.get(extractId);
        if (blankTemplateFile == null || !Files.exists(blankTemplateFile.path())) {
            return new BlankTemplateArtifacts(null, null, null);
        }
        Long blankTemplateFileId = createDerivedTemplateFileAsset(
                "TEMPLATE_EXTRACT_BLANK",
                blankTemplateFile.fileName(),
                originalFileName,
                blankTemplateFile.path(),
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                extractId,
                companyId,
                userId,
                "template-extract/" + companyId + "/" + extractId + "/blank/" + blankTemplateFile.fileName()
        ).getId();
        Long previewFileId = null;
        String previewContentType = blankTemplateFile.previewContentType();
        if (blankTemplateFile.previewPath() != null && Files.exists(blankTemplateFile.previewPath()) && StringUtils.hasText(blankTemplateFile.previewContentType())) {
            previewFileId = createDerivedTemplateFileAsset(
                    "TEMPLATE_EXTRACT_PREVIEW",
                    blankTemplateFile.previewFileName(),
                    originalFileName,
                    blankTemplateFile.previewPath(),
                    blankTemplateFile.previewContentType(),
                    extractId + "-preview",
                    companyId,
                    userId,
                    "template-extract/" + companyId + "/" + extractId + "/preview/" + blankTemplateFile.previewFileName()
            ).getId();
        }
        return new BlankTemplateArtifacts(blankTemplateFileId, previewFileId, previewContentType);
    }

    private FileAssetEntity createDerivedTemplateFileAsset(
            String bizType,
            String fileName,
            String originalName,
            Path filePath,
            String contentType,
            String fileHash,
            Long companyId,
            Long userId,
            String objectKey
    ) {
        StoredObject storedObject = objectStorageService.put(objectKey, filePath, contentType);
        FileAssetEntity entity = new FileAssetEntity();
        entity.setCompanyId(companyId);
        entity.setBizType(bizType);
        entity.setFileName(limitText(fileName, 255));
        entity.setOriginalName(limitText(safeFileName(originalName), 255));
        entity.setContentType(contentType);
        entity.setFileSize(storedObject.size());
        entity.setStorageType(storedObject.storageType());
        entity.setBucketName(storedObject.bucketName());
        entity.setObjectKey(limitText(storedObject.objectKey(), 255));
        entity.setFileHash(limitText(fileHash, 64));
        entity.setStatus(1);
        entity.setCreatedBy(userId);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        entity.setDeleted(0);
        fileAssetMapper.insert(entity);
        return entity;
    }

    private BlankTemplateFile loadAsyncBlankTemplate(String extractIdOrTaskNo) {
        return loadAsyncBlankTemplate(extractIdOrTaskNo, currentCompanyId());
    }

    private BlankTemplateFile loadAsyncBlankTemplate(String extractIdOrTaskNo, Long companyId) {
        Optional<BlParseTaskEntity> taskOptional = findAsyncTemplateExtractTask(extractIdOrTaskNo);
        if (taskOptional.isEmpty()) {
            return loadPersistedBlankTemplateAsset(extractIdOrTaskNo, companyId);
        }
        BlParseTaskEntity task = taskOptional.get();
        if (!"SUCCESS".equals(task.getTaskStatus()) || task.getResultFileId() == null) {
            return loadPersistedBlankTemplateAsset(extractIdOrTaskNo, companyId);
        }
        FileAssetEntity blankAsset = fileAssetMapper.selectById(task.getResultFileId());
        if (blankAsset == null || !StringUtils.hasText(blankAsset.getObjectKey())) {
            return null;
        }
        Path blankPath = objectStorageService.downloadToTemp(blankAsset.getBucketName(), blankAsset.getObjectKey(), blankAsset.getFileName());
        TemplateExtractTaskPayload payload = readAsyncTemplateExtractPayload(task.getResultPayload());
        if (payload == null || payload.previewFileId() == null) {
            return new BlankTemplateFile(blankAsset.getFileName(), blankPath);
        }
        FileAssetEntity previewAsset = fileAssetMapper.selectById(payload.previewFileId());
        if (previewAsset == null || !StringUtils.hasText(previewAsset.getObjectKey())) {
            return new BlankTemplateFile(blankAsset.getFileName(), blankPath);
        }
        Path previewPath = objectStorageService.downloadToTemp(previewAsset.getBucketName(), previewAsset.getObjectKey(), previewAsset.getFileName());
        return new BlankTemplateFile(
                blankAsset.getFileName(),
                blankPath,
                previewAsset.getFileName(),
                previewAsset.getContentType(),
                previewPath
        );
    }

    private BlankTemplateFile loadPersistedBlankTemplateAsset(String extractId, Long companyId) {
        FileAssetEntity blankAsset = fileAssetMapper.selectOne(new LambdaQueryWrapper<FileAssetEntity>()
                .eq(FileAssetEntity::getBizType, "TEMPLATE_EXTRACT_BLANK")
                .eq(FileAssetEntity::getFileHash, extractId)
                .eq(companyId != null, FileAssetEntity::getCompanyId, companyId)
                .eq(FileAssetEntity::getDeleted, 0)
                .orderByDesc(FileAssetEntity::getId)
                .last("LIMIT 1"));
        if (blankAsset == null || !StringUtils.hasText(blankAsset.getObjectKey())) {
            return null;
        }
        Path blankPath = objectStorageService.downloadToTemp(blankAsset.getBucketName(), blankAsset.getObjectKey(), blankAsset.getFileName());
        FileAssetEntity previewAsset = fileAssetMapper.selectOne(new LambdaQueryWrapper<FileAssetEntity>()
                .eq(FileAssetEntity::getBizType, "TEMPLATE_EXTRACT_PREVIEW")
                .eq(FileAssetEntity::getFileHash, extractId + "-preview")
                .eq(companyId != null, FileAssetEntity::getCompanyId, companyId)
                .eq(FileAssetEntity::getDeleted, 0)
                .orderByDesc(FileAssetEntity::getId)
                .last("LIMIT 1"));
        if (previewAsset == null || !StringUtils.hasText(previewAsset.getObjectKey())) {
            return new BlankTemplateFile(blankAsset.getFileName(), blankPath);
        }
        Path previewPath = objectStorageService.downloadToTemp(previewAsset.getBucketName(), previewAsset.getObjectKey(), previewAsset.getFileName());
        return new BlankTemplateFile(
                blankAsset.getFileName(),
                blankPath,
                previewAsset.getFileName(),
                previewAsset.getContentType(),
                previewPath
        );
    }

    private Optional<TemplateExtractTaskPayload> findAsyncTemplateExtractPayload(String extractIdOrTaskNo) {
        return findAsyncTemplateExtractTask(extractIdOrTaskNo)
                .map(task -> readAsyncTemplateExtractPayload(task.getResultPayload()));
    }

    private Optional<BlParseTaskEntity> findAsyncTemplateExtractTask(String extractIdOrTaskNo) {
        BlParseTaskEntity byTaskNo = taskMapper.selectOne(new LambdaQueryWrapper<BlParseTaskEntity>()
                .eq(BlParseTaskEntity::getTaskType, "TEMPLATE_EXTRACT")
                .eq(BlParseTaskEntity::getTaskNo, extractIdOrTaskNo)
                .last("LIMIT 1"));
        if (byTaskNo != null) {
            return Optional.of(byTaskNo);
        }
        return Optional.ofNullable(taskMapper.selectOne(new LambdaQueryWrapper<BlParseTaskEntity>()
                .eq(BlParseTaskEntity::getTaskType, "TEMPLATE_EXTRACT")
                .eq(BlParseTaskEntity::getFileHash, extractIdOrTaskNo)
                .last("LIMIT 1")));
    }

    private TemplateExtractTaskPayload readAsyncTemplateExtractPayload(String payload) {
        if (!StringUtils.hasText(payload)) {
            return null;
        }
        try {
            return objectMapper.readValue(payload, TemplateExtractTaskPayload.class);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR.getCode(), "模板提取任务结果读取失败");
        }
    }

    private TemplateEntity createTemplate(TemplateExtractSaveRequest request) {
        return createTemplate(request, currentCompanyId(), currentUserId());
    }

    private TemplateEntity createTemplate(TemplateExtractSaveRequest request, Long companyId, Long userId) {
        TemplateEntity entity = new TemplateEntity();
        entity.setCompanyId(companyId);
        entity.setTemplateCode("TPL_" + System.currentTimeMillis());
        entity.setTemplateName(limitText(safeText(request.templateName(), stripExtension(safeFileName(request.fileName()))), 128));
        entity.setTemplateType(limitText(safeText(request.templateType(), "BILL_DOCX"), 32));
        entity.setStatus(1);
        entity.setRemark("用户确认保存提单模板");
        entity.setCreatedBy(userId);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedBy(userId);
        entity.setUpdatedAt(LocalDateTime.now());
        entity.setDeleted(0);
        templateMapper.insert(entity);
        return entity;
    }

    private boolean isPreviewOnlyTemplate(TemplateExtractSaveRequest request) {
        return "BILL_PREVIEW".equalsIgnoreCase(safeText(request.templateType(), ""));
    }

    private TemplateVersionEntity createTemplateVersion(
            Long templateId,
            Long fileAssetId,
            List<TemplateFieldMappingSaveRequest> mappings,
            String contentFormat
    ) {
        return createTemplateVersion(templateId, fileAssetId, mappings, contentFormat, currentUserId());
    }

    private TemplateVersionEntity createTemplateVersion(
            Long templateId,
            Long fileAssetId,
            List<TemplateFieldMappingSaveRequest> mappings,
            String contentFormat,
            Long userId
    ) {
        TemplateVersionEntity entity = new TemplateVersionEntity();
        entity.setTemplateId(templateId);
        entity.setVersionNo(1);
        entity.setFileAssetId(fileAssetId);
        entity.setContentFormat(limitText(safeText(contentFormat, "DOCX"), 32));
        entity.setFieldSchemaJson(toTemplateSchemaJson(mappings));
        entity.setStatus(1);
        entity.setCreatedBy(userId);
        entity.setCreatedAt(LocalDateTime.now());
        templateVersionMapper.insert(entity);
        return entity;
    }

    private void saveFieldMappings(Long templateVersionId, List<TemplateFieldMappingSaveRequest> mappings) {
        for (int index = 0; index < mappings.size(); index++) {
            TemplateFieldMappingSaveRequest item = mappings.get(index);
            TemplateFieldMappingEntity entity = new TemplateFieldMappingEntity();
            entity.setTemplateVersionId(templateVersionId);
            entity.setFieldKey(limitText(safeText(item.placeholderKey(), "field_" + (index + 1)), 64));
            entity.setFieldName(limitText(safeText(item.description(), safeText(item.placeholderKey(), "字段" + (index + 1))), 128));
            entity.setSourceText("");
            entity.setDataType(limitText(safeText(item.dataType(), "STRING").toUpperCase(), 32));
            entity.setRequiredFlag(0);
            entity.setDefaultValue(null);
            entity.setSortNo(item.sortNo() == null ? index + 1 : item.sortNo());
            fieldMappingMapper.insert(entity);
        }
    }

    private BlankTemplateResult generateBlankTemplateIfSupported(
            String extractId,
            String originalFileName,
            MultipartFile file,
            List<TemplateFieldMappingVO> mappings
    ) {
        try {
            Path workDir = Files.createTempDirectory("manifest-template-" + extractId + "-");
            Path inputPath = workDir.resolve("source-" + safeFileName(originalFileName));
            Path mappingPath = workDir.resolve("mappings.json");
            Path outputPath = workDir.resolve("blank-template.docx");
            file.transferTo(inputPath);
            objectMapper.writeValue(mappingPath.toFile(), mappings);

            Path sourceDocxPath = resolveSourceDocx(inputPath, originalFileName, workDir);
            BlankTemplateResult result = generateBlankTemplateFromDocx(sourceDocxPath, mappingPath, outputPath, originalFileName, extractId);
            if (!"GENERATED".equals(result.status())) {
                return result;
            }

            String blankFileName = stripExtension(safeFileName(originalFileName)) + "-blank-template.docx";
            TemplatePreviewFile previewFile = createBlankTemplatePreview(outputPath, blankFileName, workDir);
            blankTemplateCache.put(extractId, new BlankTemplateFile(
                    blankFileName,
                    outputPath,
                    previewFile.fileName(),
                    previewFile.contentType(),
                    previewFile.path()
            ));
            return new BlankTemplateResult(
                    "GENERATED",
                    sourceDocxPath.equals(inputPath) ? "已完成占位符替换，可预览并确认保存。" : "已先通过 LibreOffice 转为 DOCX，并完成占位符替换。",
                    "/user/templates/extract/" + extractId + "/blank-template",
                    "/user/templates/extract/" + extractId + "/preview",
                    previewFile.contentType()
            );
        } catch (IOException ex) {
            return new BlankTemplateResult("FAILED", "DOCX 空白模板生成失败：文件处理异常。", null);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return new BlankTemplateResult("FAILED", "DOCX 空白模板生成被中断。", null);
        } catch (BusinessException ex) {
            log.warn("Blank template generation skipped, extractId={}, fileName={}, message={}", extractId, originalFileName, ex.getMessage());
            return new BlankTemplateResult("PREVIEW_ONLY", ex.getMessage(), null);
        }
    }

    private Path resolveSourceDocx(Path inputPath, String originalFileName, Path workDir) throws IOException, InterruptedException {
        String lowerName = originalFileName.toLowerCase();
        if (lowerName.endsWith(".docx")) {
            return inputPath;
        }
        if (lowerName.endsWith(".pdf")) {
            return convertPdfToDocx(inputPath, workDir);
        }
        if (lowerName.endsWith(".doc")) {
            return convertToDocx(inputPath, workDir);
        }
        throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "当前仅支持 PDF、DOC、DOCX 生成模板");
    }

    private Path convertPdfToDocx(Path inputPath, Path workDir) throws IOException, InterruptedException {
        Path outputPath = workDir.resolve(stripExtension(inputPath.getFileName().toString()) + ".docx");
        try {
            Process process = new ProcessBuilder(
                    "python3",
                    resolvePdfToDocxScriptPath(),
                    inputPath.toString(),
                    outputPath.toString()
            ).redirectErrorStream(true).start();
            String processOutput = new String(process.getInputStream().readAllBytes());
            int exitCode = process.waitFor();
            if (exitCode == 0 && Files.exists(outputPath)) {
                return outputPath;
            }
            log.warn("pdf2docx conversion failed, exitCode={}, output={}", exitCode, processOutput);
        } catch (IOException ex) {
            log.warn("pdf2docx conversion command failed, path={}", inputPath, ex);
        }
        throw new BusinessException(
                ErrorCode.BAD_REQUEST.getCode(),
                "PDF 已完成字段提取，但当前环境缺少可用的 PDF 转 DOCX 组件，暂不能生成可保存模板；请先上传 DOCX 样本，或安装 pdf2docx 后重试"
        );
    }

    private Path convertToDocx(Path inputPath, Path workDir) throws IOException, InterruptedException {
        String soffice = resolveSofficeCommand();
        if (!StringUtils.hasText(soffice)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "当前环境未安装 LibreOffice/soffice，无法先将 PDF/DOC 转为 DOCX");
        }
        Process process = new ProcessBuilder(
                soffice,
                "--headless",
                "--convert-to",
                "docx",
                "--outdir",
                workDir.toString(),
                inputPath.toString()
        ).redirectErrorStream(true).start();
        String processOutput = new String(process.getInputStream().readAllBytes());
        int exitCode = process.waitFor();
        Path outputPath = workDir.resolve(stripExtension(inputPath.getFileName().toString()) + ".docx");
        if (exitCode != 0 || !Files.exists(outputPath)) {
            log.warn("Source file to DOCX conversion failed, exitCode={}, output={}", exitCode, processOutput);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR.getCode(), "PDF/DOC 转 DOCX 失败，请检查 LibreOffice 是否支持该文件");
        }
        return outputPath;
    }

    private BlankTemplateResult generateBlankTemplateFromDocx(
            Path inputPath,
            Path mappingPath,
            Path outputPath,
            String originalFileName,
            String extractId
    ) throws IOException, InterruptedException {
        Process process = new ProcessBuilder(
                "python3",
                resolveScriptPath(),
                inputPath.toString(),
                mappingPath.toString(),
                outputPath.toString()
        ).redirectErrorStream(true).start();
        String processOutput = new String(process.getInputStream().readAllBytes());
        int exitCode = process.waitFor();
        if (exitCode != 0 || !Files.exists(outputPath)) {
            log.warn("DOCX blank template generation failed, extractId={}, fileName={}, exitCode={}, output={}",
                    extractId, originalFileName, exitCode, processOutput);
            return new BlankTemplateResult("FAILED", "DOCX 空白模板生成失败，请检查 python-docx 环境或转换后的 DOCX 内容。", null);
        }
        return new BlankTemplateResult("GENERATED", "已生成可下载的 DOCX 空白模板。", null);
    }

    private TemplatePreviewFile createBlankTemplatePreview(Path docxPath, String docxFileName, Path workDir) {
        try {
            Path pdfPath = convertDocxToPdf(docxPath, workDir);
            return new TemplatePreviewFile(
                    stripExtension(docxFileName) + "-preview.pdf",
                    "application/pdf",
                    pdfPath
            );
        } catch (RuntimeException | IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.warn("Blank template PDF preview generation failed, fallback to DOCX preview, path={}", docxPath, ex);
            return new TemplatePreviewFile(
                    docxFileName,
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    docxPath
            );
        }
    }

    private BlankTemplateResult generatePlaceholderTemplate(
            String extractId,
            String originalFileName,
            List<TemplateFieldMappingVO> mappings
    ) {
        if (mappings == null || mappings.isEmpty()) {
            return new BlankTemplateResult("PREVIEW_ONLY", "当前文件先展示提取数据；Dify 未返回字段，无法生成 DOCX 模板。", null);
        }
        try {
            Path workDir = Files.createTempDirectory("manifest-template-placeholder-" + extractId + "-");
            Path mappingPath = workDir.resolve("mappings.json");
            Path outputPath = workDir.resolve("blank-template.docx");
            objectMapper.writeValue(mappingPath.toFile(), mappings);

            Process process = new ProcessBuilder(
                    "python3",
                    resolvePlaceholderScriptPath(),
                    mappingPath.toString(),
                    outputPath.toString()
            ).redirectErrorStream(true).start();
            String processOutput = new String(process.getInputStream().readAllBytes());
            int exitCode = process.waitFor();
            if (exitCode != 0 || !Files.exists(outputPath)) {
                log.warn("Placeholder DOCX template generation failed, exitCode={}, output={}", exitCode, processOutput);
                return new BlankTemplateResult("FAILED", "占位符 DOCX 模板生成失败，请检查 python-docx 环境。", null);
            }

            String blankFileName = stripExtension(safeFileName(originalFileName)) + "-blank-template.docx";
            blankTemplateCache.put(extractId, new BlankTemplateFile(blankFileName, outputPath));
            return new BlankTemplateResult(
                    "GENERATED",
                    "已根据提取字段生成可保存的 DOCX 占位符模板；如需保留原 PDF 版式，后续可接入 PDF 转 DOCX。",
                    "/user/templates/extract/" + extractId + "/blank-template"
            );
        } catch (IOException ex) {
            return new BlankTemplateResult("FAILED", "占位符 DOCX 模板生成失败：文件处理异常。", null);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return new BlankTemplateResult("FAILED", "占位符 DOCX 模板生成被中断。", null);
        }
    }

    private String resolveScriptPath() throws IOException {
        for (Path candidate : List.of(
                Path.of("service/service-user/scripts/generate_blank_docx.py"),
                Path.of("scripts/generate_blank_docx.py")
        )) {
            if (Files.exists(candidate)) {
                return candidate.toAbsolutePath().toString();
            }
        }
        return new ClassPathResource("scripts/generate_blank_docx.py").getFile().getAbsolutePath();
    }

    private String resolvePdfToDocxScriptPath() throws IOException {
        for (Path candidate : List.of(
                Path.of("service/service-user/scripts/convert_pdf_to_docx.py"),
                Path.of("scripts/convert_pdf_to_docx.py")
        )) {
            if (Files.exists(candidate)) {
                return candidate.toAbsolutePath().toString();
            }
        }
        return new ClassPathResource("scripts/convert_pdf_to_docx.py").getFile().getAbsolutePath();
    }

    private String resolvePlaceholderScriptPath() throws IOException {
        for (Path candidate : List.of(
                Path.of("service/service-user/scripts/generate_placeholder_docx.py"),
                Path.of("scripts/generate_placeholder_docx.py")
        )) {
            if (Files.exists(candidate)) {
                return candidate.toAbsolutePath().toString();
            }
        }
        return new ClassPathResource("scripts/generate_placeholder_docx.py").getFile().getAbsolutePath();
    }

    private String resolveRenderScriptPath() throws IOException {
        for (Path candidate : List.of(
                Path.of("service/service-user/scripts/render_template_docx.py"),
                Path.of("scripts/render_template_docx.py")
        )) {
            if (Files.exists(candidate)) {
                return candidate.toAbsolutePath().toString();
            }
        }
        return new ClassPathResource("scripts/render_template_docx.py").getFile().getAbsolutePath();
    }

    private boolean isSofficeAvailable() {
        return StringUtils.hasText(resolveSofficeCommand());
    }

    private String resolveSofficeCommand() {
        for (String command : List.of("soffice", "libreoffice", "/Applications/LibreOffice.app/Contents/MacOS/soffice")) {
            try {
                Process process = new ProcessBuilder(command, "--version")
                        .redirectErrorStream(true)
                        .start();
                int exitCode = process.waitFor();
                if (exitCode == 0) {
                    return command;
                }
            } catch (IOException ex) {
                log.debug("{} command is not available", command);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return "";
            }
        }
        return "";
    }

    private String hashFile(MultipartFile file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(file.getBytes()));
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR.getCode(), "读取上传文件失败");
        } catch (NoSuchAlgorithmException ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR.getCode(), "文件指纹算法不可用");
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR.getCode(), "字段映射序列化失败");
        }
    }

    private String safeJson(TemplateExtractSaveRequest request) {
        List<TemplateFieldMappingSaveRequest> mappings = request.mappings() == null ? List.of() : request.mappings();
        return toTemplateSchemaJson(mappings);
    }

    private String toTemplateSchemaJson(List<TemplateFieldMappingSaveRequest> mappings) {
        List<Map<String, Object>> templateSchema = mappings.stream()
                .map(item -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("placeholderKey", item.placeholderKey());
                    row.put("dataType", item.dataType());
                    row.put("description", item.description());
                    row.put("sortNo", item.sortNo());
                    return row;
                })
                .toList();
        return toJson(templateSchema);
    }

    private String safeText(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    private String safeFileName(String fileName) {
        return safeText(fileName, "template-extract.json");
    }

    private Long resolveTemplateFileSize(BlankTemplateFile blankTemplateFile, TemplateExtractSaveRequest request) {
        if (blankTemplateFile != null) {
            try {
                return Files.size(blankTemplateFile.path());
            } catch (IOException ex) {
                log.warn("Read generated blank template size failed, path={}", blankTemplateFile.path(), ex);
            }
        }
        return (long) safeJson(request).length();
    }

    private LambdaQueryWrapper<TemplateEntity> templateQueryWrapper(TemplatePageQuery query) {
        LambdaQueryWrapper<TemplateEntity> wrapper = new LambdaQueryWrapper<TemplateEntity>()
                .eq(TemplateEntity::getDeleted, 0)
                .and(item -> item.eq(TemplateEntity::getCompanyId, currentCompanyId()).or().isNull(TemplateEntity::getCompanyId))
                .orderByDesc(TemplateEntity::getUpdatedAt);
        if (query.status() != null) {
            wrapper.eq(TemplateEntity::getStatus, query.status());
        }
        if (StringUtils.hasText(query.keyword())) {
            wrapper.and(item -> item.like(TemplateEntity::getTemplateName, query.keyword())
                    .or()
                    .like(TemplateEntity::getTemplateCode, query.keyword()));
        }
        return wrapper;
    }

    private TemplateManageVO toManageVO(TemplateEntity template) {
        TemplateVersionEntity version = findCurrentVersion(template);
        FileAssetEntity fileAsset = version == null ? null : fileAssetMapper.selectById(version.getFileAssetId());
        Integer fieldCount = version == null
                ? 0
                : Math.toIntExact(fieldMappingMapper.selectCount(new LambdaQueryWrapper<TemplateFieldMappingEntity>()
                        .eq(TemplateFieldMappingEntity::getTemplateVersionId, version.getId())));
        return new TemplateManageVO(
                template.getId(),
                template.getTemplateCode(),
                template.getTemplateName(),
                template.getTemplateType(),
                template.getStatus(),
                template.getCurrentVersionId(),
                version == null ? null : version.getVersionNo(),
                fileAsset == null ? null : fileAsset.getId(),
                fileAsset == null ? null : fileAsset.getFileName(),
                fileAsset == null ? null : fileAsset.getStorageType(),
                fileAsset == null ? null : fileAsset.getObjectKey(),
                version == null ? null : version.getContentFormat(),
                fieldCount,
                template.getCreatedAt(),
                template.getUpdatedAt()
        );
    }

    private TemplateVersionEntity findCurrentVersion(TemplateEntity template) {
        if (template.getCurrentVersionId() != null) {
            TemplateVersionEntity version = templateVersionMapper.selectById(template.getCurrentVersionId());
            if (version != null) {
                return version;
            }
        }
        return templateVersionMapper.selectOne(new LambdaQueryWrapper<TemplateVersionEntity>()
                .eq(TemplateVersionEntity::getTemplateId, template.getId())
                .orderByDesc(TemplateVersionEntity::getVersionNo)
                .last("LIMIT 1"));
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

    private String stripExtension(String fileName) {
        int index = fileName.lastIndexOf('.');
        return index > 0 ? fileName.substring(0, index) : fileName;
    }

    private String limitText(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private record BlankTemplateResult(
            String status,
            String message,
            String downloadUrl,
            String previewUrl,
            String previewContentType
    ) {
        private BlankTemplateResult(String status, String message, String downloadUrl) {
            this(status, message, downloadUrl, null, null);
        }
    }

    private record TemplatePreviewFile(
            String fileName,
            String contentType,
            Path path
    ) {
    }

    private record ExportContext(
            TemplateEntity template,
            TemplateVersionEntity version,
            FileAssetEntity fileAsset,
            Path templatePath
    ) {
    }

    private record RenderedTemplate(
            String exportId,
            String fileName,
            String contentType,
            Path path
    ) {
    }

    private record BlankTemplateArtifacts(
            Long blankTemplateFileId,
            Long previewFileId,
            String previewContentType
    ) {
    }

    private record TemplateExtractTaskPayload(
            TemplateExtractResultVO result,
            Long previewFileId
    ) {
    }

    record TemplateExtractExecutionResult(
            TemplateExtractResultVO result,
            Long blankTemplateFileId,
            Long previewFileId
    ) {
    }

    record TemplateExportExecutionResult(
            TemplateExportResultVO result,
            Long fileAssetId
    ) {
    }
}
