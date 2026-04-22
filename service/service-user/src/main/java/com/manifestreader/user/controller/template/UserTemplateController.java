package com.manifestreader.user.controller.template;

import com.manifestreader.common.result.PageResult;
import com.manifestreader.common.result.R;
import com.manifestreader.user.model.dto.TemplateExtractSaveRequest;
import com.manifestreader.user.model.dto.TemplatePageQuery;
import com.manifestreader.user.model.dto.TemplateStatusUpdateRequest;
import com.manifestreader.user.model.vo.BlankTemplateFile;
import com.manifestreader.user.model.vo.ExportedTemplateFile;
import com.manifestreader.user.model.vo.TemplateExtractResultVO;
import com.manifestreader.user.model.vo.TemplateExtractSaveResultVO;
import com.manifestreader.user.model.vo.TemplateExportResultVO;
import com.manifestreader.user.model.vo.TemplateManageVO;
import com.manifestreader.user.model.vo.TemplateOptionVO;
import com.manifestreader.user.service.UserTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "用户端-模板")
@RestController
@RequestMapping("/user/templates")
public class UserTemplateController {

    private final UserTemplateService templateService;

    public UserTemplateController(UserTemplateService templateService) {
        this.templateService = templateService;
    }

    @Operation(summary = "模板分页")
    @GetMapping("/page")
    public R<PageResult<TemplateOptionVO>> page(TemplatePageQuery query) {
        return R.ok(templateService.page(query));
    }

    @Operation(summary = "模板详情")
    @GetMapping("/{id}")
    public R<TemplateOptionVO> detail(@PathVariable Long id) {
        return R.ok(templateService.detail(id));
    }

    @Operation(summary = "可用模板列表")
    @GetMapping("/usable")
    public R<List<TemplateOptionVO>> usableTemplates() {
        return R.ok(templateService.usableTemplates());
    }

    @Operation(summary = "模板管理分页")
    @GetMapping("/manage/page")
    public R<PageResult<TemplateManageVO>> managePage(TemplatePageQuery query) {
        return R.ok(templateService.managePage(query));
    }

    @Operation(summary = "模板管理详情")
    @GetMapping("/manage/{id}")
    public R<TemplateManageVO> manageDetail(@PathVariable Long id) {
        return R.ok(templateService.manageDetail(id));
    }

    @Operation(summary = "修改模板状态")
    @PutMapping("/manage/{id}/status")
    public R<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody TemplateStatusUpdateRequest request) {
        templateService.updateStatus(id, request);
        return R.ok();
    }

    @Operation(summary = "删除模板")
    @DeleteMapping("/manage/{id}")
    public R<Void> deleteTemplate(@PathVariable Long id) {
        templateService.deleteTemplate(id);
        return R.ok();
    }

    @Operation(summary = "上传样本并调用 Dify 提取模板字段")
    @PostMapping(value = "/extract", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<TemplateExtractResultVO> extract(@RequestPart("file") MultipartFile file) {
        return R.ok(templateService.extractTemplate(file));
    }

    @Operation(summary = "确认保存模板配置")
    @PostMapping("/extract/save")
    public R<TemplateExtractSaveResultVO> saveGeneratedTemplate(@Valid @RequestBody TemplateExtractSaveRequest request) {
        return R.ok(templateService.saveGeneratedTemplate(request));
    }

    @Operation(summary = "下载空白 DOCX 模板")
    @GetMapping("/extract/{extractId}/blank-template")
    public ResponseEntity<Resource> downloadBlankTemplate(@PathVariable String extractId) {
        BlankTemplateFile file = templateService.getBlankTemplate(extractId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.fileName() + "\"")
                .body(new FileSystemResource(file.path()));
    }

    @Operation(summary = "按模板导出文件")
    @PostMapping(value = "/export", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<TemplateExportResultVO> exportWithTemplate(
            @RequestParam("templateId") Long templateId,
            @RequestParam(value = "outputFormat", defaultValue = "DOCX") String outputFormat,
            @RequestPart("file") MultipartFile file
    ) {
        return R.ok(templateService.exportWithTemplate(templateId, outputFormat, file));
    }

    @Operation(summary = "下载按模板导出的文件")
    @GetMapping("/export/{exportId}/download")
    public ResponseEntity<Resource> downloadExportedTemplate(@PathVariable String exportId) {
        ExportedTemplateFile file = templateService.getExportedTemplate(exportId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.fileName() + "\"")
                .body(new FileSystemResource(file.path()));
    }
}
