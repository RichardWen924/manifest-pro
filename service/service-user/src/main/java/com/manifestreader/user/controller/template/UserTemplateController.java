package com.manifestreader.user.controller.template;

import com.manifestreader.common.result.PageResult;
import com.manifestreader.common.result.R;
import com.manifestreader.user.model.dto.TemplatePageQuery;
import com.manifestreader.user.model.vo.TemplateExtractResultVO;
import com.manifestreader.user.model.vo.TemplateOptionVO;
import com.manifestreader.user.service.UserTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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

    @Operation(summary = "上传样本并调用 Dify 提取模板字段")
    @PostMapping(value = "/extract", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<TemplateExtractResultVO> extract(@RequestPart("file") MultipartFile file) {
        return R.ok(templateService.extractTemplate(file));
    }
}
