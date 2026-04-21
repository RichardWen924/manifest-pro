package com.manifestreader.admin.service.log;

import com.manifestreader.common.result.PageResult;
import com.manifestreader.model.entity.OperLogEntity;
import com.manifestreader.model.query.BasePageQuery;
import org.springframework.stereotype.Service;

@Service
public class LogAdminServiceImpl implements LogAdminService {

    @Override
    public PageResult<OperLogEntity> pageLogs(BasePageQuery query) {
        return PageResult.empty(query.getCurrent(), query.getSize());
    }
}
