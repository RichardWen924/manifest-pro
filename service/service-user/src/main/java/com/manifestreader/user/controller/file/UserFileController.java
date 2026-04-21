package com.manifestreader.user.controller.file;

import com.manifestreader.common.result.R;
import com.manifestreader.user.model.dto.FileUploadInitRequest;
import com.manifestreader.user.model.vo.FileAssetVO;
import com.manifestreader.user.service.UserFileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    @Operation(summary = "文件详情")
    @GetMapping("/{id}")
    public R<FileAssetVO> detail(@PathVariable Long id) {
        return R.ok(fileService.detail(id));
    }

    @Operation(summary = "删除文件")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        fileService.delete(id);
        return R.ok();
    }
}
