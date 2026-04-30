package com.manifestreader.user.service;

import com.manifestreader.common.result.PageResult;
import com.manifestreader.user.model.dto.ConsolidatedQuery;
import com.manifestreader.user.model.vo.ConsolidatedDataVO;

public interface ConsolidatedService {

    PageResult<ConsolidatedDataVO> page(ConsolidatedQuery query);

    ConsolidatedDataVO detail(Long billId);
}
