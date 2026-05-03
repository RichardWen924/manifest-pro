package com.manifestreader.admin.service.market;

import com.manifestreader.admin.feign.MarketAdminFeignClient;
import com.manifestreader.common.exception.BizException;
import com.manifestreader.common.result.PageResult;
import com.manifestreader.common.result.R;
import com.manifestreader.model.dto.MarketDemandAuditRequest;
import com.manifestreader.model.vo.MarketDemandAdminVO;
import org.springframework.stereotype.Service;

@Service
public class MarketAdminServiceImpl implements MarketAdminService {

    private final MarketAdminFeignClient marketAdminFeignClient;

    public MarketAdminServiceImpl(MarketAdminFeignClient marketAdminFeignClient) {
        this.marketAdminFeignClient = marketAdminFeignClient;
    }

    @Override
    public PageResult<MarketDemandAdminVO> pageDemands(String keyword, String auditStatus, Long pageNo, Long pageSize) {
        R<PageResult<MarketDemandAdminVO>> response = marketAdminFeignClient.pageDemands(keyword, auditStatus, pageNo, pageSize);
        if (response == null || !response.isSuccess() || response.getData() == null) {
            String message = response == null ? "商城服务无响应" : response.getMessage();
            throw new BizException("MARKET_SERVICE_UNAVAILABLE", message);
        }
        return response.getData();
    }

    @Override
    public MarketDemandAdminVO auditDemand(Long demandId, MarketDemandAuditRequest request) {
        R<MarketDemandAdminVO> response = marketAdminFeignClient.auditDemand(demandId, request);
        if (response == null || !response.isSuccess() || response.getData() == null) {
            String message = response == null ? "商城服务无响应" : response.getMessage();
            throw new BizException("MARKET_SERVICE_UNAVAILABLE", message);
        }
        return response.getData();
    }
}
