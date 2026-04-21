package com.manifestreader.admin.service.template;

import com.manifestreader.common.result.PageResult;
import com.manifestreader.model.entity.TemplateEntity;
import com.manifestreader.model.query.TemplateQuery;
import org.springframework.stereotype.Service;

@Service
public class TemplateAdminServiceImpl implements TemplateAdminService {

    @Override
    public PageResult<TemplateEntity> pageTemplates(TemplateQuery query) {
        return PageResult.empty(query.getCurrent(), query.getSize());
    }
}
