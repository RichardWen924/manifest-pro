package com.manifestreader.user.service.impl;

import com.manifestreader.common.result.PageResult;
import com.manifestreader.user.model.dto.ConsolidatedQuery;
import com.manifestreader.user.model.vo.ConsolidatedDataVO;
import com.manifestreader.user.service.ConsolidatedService;
import java.util.Collections;
import org.springframework.stereotype.Service;

@Service
public class ConsolidatedServiceImpl implements ConsolidatedService {

    @Override
    public PageResult<ConsolidatedDataVO> page(ConsolidatedQuery query) {
        return PageResult.empty(query.pageNo(), query.pageSize());
    }

    @Override
    public ConsolidatedDataVO detail(Long billId) {
        return new ConsolidatedDataVO(billId, null, Collections.emptyList(), Collections.emptyList());
    }
}
