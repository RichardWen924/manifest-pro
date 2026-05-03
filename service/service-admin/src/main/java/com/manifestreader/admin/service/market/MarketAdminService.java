package com.manifestreader.admin.service.market;

import com.manifestreader.common.result.PageResult;
import com.manifestreader.model.dto.MarketDemandAuditRequest;
import com.manifestreader.model.vo.MarketDemandAdminVO;

public interface MarketAdminService {

    PageResult<MarketDemandAdminVO> pageDemands(String keyword, String auditStatus, Long pageNo, Long pageSize);

    MarketDemandAdminVO auditDemand(Long demandId, MarketDemandAuditRequest request);
}
