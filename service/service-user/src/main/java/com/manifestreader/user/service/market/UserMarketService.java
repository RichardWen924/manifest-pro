package com.manifestreader.user.service.market;

import com.manifestreader.common.result.PageResult;
import com.manifestreader.model.dto.FreightDemandAcceptRequest;
import com.manifestreader.model.dto.FreightDemandCreateRequest;
import com.manifestreader.model.dto.FreightDemandPageQuery;
import com.manifestreader.model.dto.FreightQuoteCreateRequest;
import com.manifestreader.model.vo.FreightDemandDetailVO;
import com.manifestreader.model.vo.FreightDemandVO;
import com.manifestreader.model.vo.FreightOrderVO;
import com.manifestreader.model.vo.FreightQuoteVO;
import java.util.List;

public interface UserMarketService {

    FreightDemandVO createDemand(FreightDemandCreateRequest request);

    PageResult<FreightDemandVO> pageDemands(FreightDemandPageQuery query);

    PageResult<FreightDemandVO> pageMineDemands(FreightDemandPageQuery query);

    FreightDemandDetailVO detailDemand(Long demandId);

    FreightDemandVO cancelDemand(Long demandId);

    FreightQuoteVO submitQuote(Long demandId, FreightQuoteCreateRequest request);

    List<FreightQuoteVO> listQuotes(Long demandId);

    FreightQuoteVO withdrawQuote(Long demandId, Long quoteId);

    FreightOrderVO acceptQuote(Long demandId, FreightDemandAcceptRequest request);

    PageResult<FreightOrderVO> pageMyAcceptedOrders(Long pageNo, Long pageSize);

    FreightOrderVO startOrder(Long orderId);

    FreightOrderVO completeOrder(Long orderId);
}
