package com.manifestreader.admin.controller.template;

import com.manifestreader.admin.service.template.TemplateAdminService;
import com.manifestreader.common.result.PageResult;
import com.manifestreader.common.result.Result;
import com.manifestreader.model.entity.TemplateEntity;
import com.manifestreader.model.query.TemplateQuery;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/template")
public class TemplateController {

    private final TemplateAdminService templateAdminService;

    public TemplateController(TemplateAdminService templateAdminService) {
        this.templateAdminService = templateAdminService;
    }

    @GetMapping("/page")
    public Result<PageResult<TemplateEntity>> page(TemplateQuery query) {
        return Result.success(templateAdminService.pageTemplates(query));
    }
}
