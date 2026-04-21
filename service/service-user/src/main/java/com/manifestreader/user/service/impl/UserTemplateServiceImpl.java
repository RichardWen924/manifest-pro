package com.manifestreader.user.service.impl;

import com.manifestreader.common.result.PageResult;
import com.manifestreader.user.model.dto.TemplatePageQuery;
import com.manifestreader.user.model.vo.TemplateOptionVO;
import com.manifestreader.user.service.UserTemplateService;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class UserTemplateServiceImpl implements UserTemplateService {

    @Override
    public PageResult<TemplateOptionVO> page(TemplatePageQuery query) {
        return PageResult.empty(query.pageNo(), query.pageSize());
    }

    @Override
    public TemplateOptionVO detail(Long id) {
        return new TemplateOptionVO(id, null, null, null);
    }

    @Override
    public List<TemplateOptionVO> usableTemplates() {
        return Collections.emptyList();
    }
}
