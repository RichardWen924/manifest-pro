package com.manifestreader.market.service;

import com.manifestreader.common.result.PageResult;
import com.manifestreader.model.dto.FreightDemandAcceptRequest;
import com.manifestreader.model.dto.FreightDemandCreateRequest;
import com.manifestreader.model.dto.FreightDemandPageQuery;
import com.manifestreader.model.dto.FreightQuoteCreateRequest;
import com.manifestreader.model.vo.FreightDemandDetailVO;
import com.manifestreader.model.vo.FreightDemandVO;
import com.manifestreader.model.vo.FreightOrderVO;
import com.manifestreader.model.vo.FreightQuoteVO;
import com.manifestreader.model.dto.MarketDemandAuditRequest;
import com.manifestreader.model.vo.MarketDemandAdminVO;
import java.util.List;

public interface FreightDemandService {

    FreightDemandVO createDemand(FreightDemandCreateRequest request);

    PageResult<FreightDemandVO> page(FreightDemandPageQuery query);

    PageResult<FreightDemandVO> pageMineDemands(FreightDemandPageQuery query);

    FreightDemandDetailVO detail(Long id);

    FreightDemandVO cancelDemand(Long demandId);

    FreightQuoteVO submitQuote(Long demandId, FreightQuoteCreateRequest request);

    List<FreightQuoteVO> listQuotes(Long demandId);

    FreightQuoteVO withdrawQuote(Long demandId, Long quoteId);

    FreightOrderVO acceptQuote(Long demandId, FreightDemandAcceptRequest request);

    PageResult<FreightOrderVO> pageMyAcceptedOrders(Long pageNo, Long pageSize);

    FreightOrderVO startOrder(Long orderId);

    FreightOrderVO completeOrder(Long orderId);

    PageResult<MarketDemandAdminVO> pageAdminDemands(String keyword, String auditStatus, Long pageNo, Long pageSize);

    MarketDemandAdminVO auditDemand(Long demandId, MarketDemandAuditRequest request);
}
