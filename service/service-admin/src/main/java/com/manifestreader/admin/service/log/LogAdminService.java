package com.manifestreader.admin.service.log;

import com.manifestreader.common.result.PageResult;
import com.manifestreader.model.entity.OperLogEntity;
import com.manifestreader.model.query.BasePageQuery;

public interface LogAdminService {

    PageResult<OperLogEntity> pageLogs(BasePageQuery query);
}
