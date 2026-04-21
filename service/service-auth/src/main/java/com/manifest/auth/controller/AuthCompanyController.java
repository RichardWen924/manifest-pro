package com.manifest.auth.controller;

import com.manifest.auth.dto.CompanyCreateRequest;
import com.manifest.auth.dto.CompanyPageQuery;
import com.manifest.auth.dto.CompanyStatusRequest;
import com.manifest.auth.dto.CompanyUpdateRequest;
import com.manifest.auth.service.AuthCompanyService;
import com.manifest.auth.vo.CompanyVO;
import com.manifestreader.common.result.PageResult;
import com.manifestreader.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "认证中心-租户")
@RestController
@RequestMapping("/auth/companies")
public class AuthCompanyController {

    private final AuthCompanyService companyService;

    public AuthCompanyController(AuthCompanyService companyService) {
        this.companyService = companyService;
    }

    @Operation(summary = "租户分页")
    @GetMapping("/page")
    public R<PageResult<CompanyVO>> page(CompanyPageQuery query) {
        return R.ok(companyService.page(query));
    }

    @Operation(summary = "租户详情")
    @GetMapping("/{id}")
    public R<CompanyVO> detail(@PathVariable Long id) {
        return R.ok(companyService.detail(id));
    }

    @Operation(summary = "创建租户")
    @PostMapping
    public R<CompanyVO> create(@Valid @RequestBody CompanyCreateRequest request) {
        return R.ok(companyService.create(request));
    }

    @Operation(summary = "更新租户")
    @PutMapping("/{id}")
    public R<CompanyVO> update(@PathVariable Long id, @Valid @RequestBody CompanyUpdateRequest request) {
        return R.ok(companyService.update(id, request));
    }

    @Operation(summary = "修改租户状态")
    @PutMapping("/{id}/status")
    public R<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody CompanyStatusRequest request) {
        companyService.updateStatus(id, request);
        return R.ok();
    }
}
