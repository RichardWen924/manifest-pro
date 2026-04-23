package com.manifestreader.user.controller.file;

import com.manifestreader.common.result.R;
import com.manifestreader.user.model.dto.FileUploadInitRequest;
import com.manifestreader.user.model.vo.FileAssetVO;
import com.manifestreader.user.model.vo.FileDownloadVO;
import com.manifestreader.user.service.UserFileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "用户端-文件")
@RestController
@RequestMapping("/user/files")
public class UserFileController {

    private final UserFileService fileService;

    public UserFileController(UserFileService fileService) {
        this.fileService = fileService;
    }

    @Operation(summary = "初始化文件上传")
    @PostMapping("/upload/init")
    public R<FileAssetVO> initUpload(@Valid @RequestBody FileUploadInitRequest request) {
        return R.ok(fileService.initUpload(request));
    }

    @Operation(summary = "上传文件到对象存储")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<FileAssetVO> upload(
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "bizType", required = false) String bizType
    ) {
        return R.ok(fileService.upload(file, bizType));
    }

    @Operation(summary = "文件详情")
    @GetMapping("/{id}")
    public R<FileAssetVO> detail(@PathVariable Long id) {
        return R.ok(fileService.detail(id));
    }

    @Operation(summary = "下载文件")
    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        FileDownloadVO file = fileService.download(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition(file.fileName()))
                .body(new FileSystemResource(file.path()));
    }

    @Operation(summary = "删除文件")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        fileService.delete(id);
        return R.ok();
    }

    private String contentDisposition(String fileName) {
        String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
        return "attachment; filename*=UTF-8''" + encoded;
    }
}
