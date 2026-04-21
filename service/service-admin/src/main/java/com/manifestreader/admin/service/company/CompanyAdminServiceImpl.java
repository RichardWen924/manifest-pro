package com.manifestreader.admin.service.company;

import com.manifestreader.common.result.PageResult;
import com.manifestreader.model.entity.CompanyEntity;
import com.manifestreader.model.query.BasePageQuery;
import org.springframework.stereotype.Service;

@Service
public class CompanyAdminServiceImpl implements CompanyAdminService {

    @Override
    public PageResult<CompanyEntity> pageCompanies(BasePageQuery query) {
        return PageResult.empty(query.getCurrent(), query.getSize());
    }

    @Override
    public void changeStatus(Long companyId, Integer status) {
        // Placeholder: database update will be implemented in a later step.
    }

    @Override
    public void changeVipStatus(Long companyId, Integer vipStatus) {
        // Placeholder: database update will be implemented in a later step.
    }
}
