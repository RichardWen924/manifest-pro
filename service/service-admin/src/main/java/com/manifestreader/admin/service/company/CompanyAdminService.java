package com.manifestreader.admin.service.company;

import com.manifestreader.common.result.PageResult;
import com.manifestreader.model.entity.CompanyEntity;
import com.manifestreader.model.query.BasePageQuery;

public interface CompanyAdminService {

    PageResult<CompanyEntity> pageCompanies(BasePageQuery query);

    void changeStatus(Long companyId, Integer status);

    void changeVipStatus(Long companyId, Integer vipStatus);
}
