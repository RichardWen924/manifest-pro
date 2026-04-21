package com.manifestreader.admin.service.template;

import com.manifestreader.common.result.PageResult;
import com.manifestreader.model.entity.TemplateEntity;
import com.manifestreader.model.query.TemplateQuery;

public interface TemplateAdminService {

    PageResult<TemplateEntity> pageTemplates(TemplateQuery query);
}
