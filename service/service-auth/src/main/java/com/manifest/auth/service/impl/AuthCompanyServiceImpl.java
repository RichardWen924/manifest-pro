package com.manifest.auth.service.impl;

import com.manifest.auth.dto.CompanyCreateRequest;
import com.manifest.auth.dto.CompanyPageQuery;
import com.manifest.auth.dto.CompanyStatusRequest;
import com.manifest.auth.dto.CompanyUpdateRequest;
import com.manifest.auth.service.AuthCompanyService;
import com.manifest.auth.vo.CompanyVO;
import com.manifestreader.common.exception.BusinessException;
import com.manifestreader.common.exception.ErrorCode;
import com.manifestreader.common.result.PageResult;
import org.springframework.stereotype.Service;

@Service
public class AuthCompanyServiceImpl implements AuthCompanyService {

    @Override
    public PageResult<CompanyVO> page(CompanyPageQuery query) {
        return PageResult.empty(query.pageNo(), query.pageSize());
    }

    @Override
    public CompanyVO detail(Long id) {
        return new CompanyVO(id, null, null, null, null, null, null, null);
    }

    @Override
    public CompanyVO create(CompanyCreateRequest request) {
        throw new BusinessException(ErrorCode.NOT_IMPLEMENTED);
    }

    @Override
    public CompanyVO update(Long id, CompanyUpdateRequest request) {
        throw new BusinessException(ErrorCode.NOT_IMPLEMENTED);
    }

    @Override
    public void updateStatus(Long id, CompanyStatusRequest request) {
        // TODO 更新 sys_company.status。
    }
}
