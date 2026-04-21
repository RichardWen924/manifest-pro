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
        PageResult<TemplateOptionVO> pageResult = PageResult.empty(query.pageNo(), query.pageSize());
        List<TemplateOptionVO> records = usableTemplates();
        pageResult.setRecords(records);
        pageResult.setTotal(records.size());
        return pageResult;
    }

    @Override
    public TemplateOptionVO detail(Long id) {
        return new TemplateOptionVO(id, null, null, null);
    }

    @Override
    public List<TemplateOptionVO> usableTemplates() {
        return List.of(
                new TemplateOptionVO(1L, "BILL_STD", "标准海运提单模板", 1),
                new TemplateOptionVO(2L, "BILL_NA", "北美线提单模板", 1),
                new TemplateOptionVO(3L, "BOOKING_EU", "欧线订舱导出模板", 1)
        );
    }
}
