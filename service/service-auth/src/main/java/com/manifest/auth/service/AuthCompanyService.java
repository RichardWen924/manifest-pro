package com.manifest.auth.service;

import com.manifest.auth.dto.CompanyCreateRequest;
import com.manifest.auth.dto.CompanyPageQuery;
import com.manifest.auth.dto.CompanyStatusRequest;
import com.manifest.auth.dto.CompanyUpdateRequest;
import com.manifest.auth.vo.CompanyVO;
import com.manifestreader.common.result.PageResult;

public interface AuthCompanyService {

    PageResult<CompanyVO> page(CompanyPageQuery query);

    CompanyVO detail(Long id);

    CompanyVO create(CompanyCreateRequest request);

    CompanyVO update(Long id, CompanyUpdateRequest request);

    void updateStatus(Long id, CompanyStatusRequest request);
}
