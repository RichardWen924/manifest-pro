package com.manifestreader.admin.controller.company;

import com.manifestreader.admin.service.company.CompanyAdminService;
import com.manifestreader.common.result.PageResult;
import com.manifestreader.common.result.Result;
import com.manifestreader.model.entity.CompanyEntity;
import com.manifestreader.model.query.BasePageQuery;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/company")
public class CompanyController {

    private final CompanyAdminService companyAdminService;

    public CompanyController(CompanyAdminService companyAdminService) {
        this.companyAdminService = companyAdminService;
    }

    @GetMapping("/page")
    public Result<PageResult<CompanyEntity>> page(BasePageQuery query) {
        return Result.success(companyAdminService.pageCompanies(query));
    }

    @PatchMapping("/{companyId}/status")
    public Result<Void> changeStatus(@PathVariable Long companyId, @RequestParam Integer status) {
        companyAdminService.changeStatus(companyId, status);
        return Result.success();
    }

    @PatchMapping("/{companyId}/vip-status")
    public Result<Void> changeVipStatus(@PathVariable Long companyId, @RequestParam Integer vipStatus) {
        companyAdminService.changeVipStatus(companyId, vipStatus);
        return Result.success();
    }
}
