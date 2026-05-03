package com.manifestreader.user.service.impl;

import com.manifestreader.common.exception.BusinessException;
import com.manifestreader.common.exception.ErrorCode;
import com.manifestreader.common.result.PageResult;
import com.manifestreader.common.result.R;
import com.manifestreader.model.dto.FreightDemandAcceptRequest;
import com.manifestreader.model.dto.FreightDemandCreateRequest;
import com.manifestreader.model.dto.FreightDemandPageQuery;
import com.manifestreader.model.dto.FreightQuoteCreateRequest;
import com.manifestreader.model.vo.FreightDemandDetailVO;
import com.manifestreader.model.vo.FreightDemandVO;
import com.manifestreader.model.vo.FreightOrderVO;
import com.manifestreader.model.vo.FreightQuoteVO;
import com.manifestreader.user.feign.MarketUserFeignClient;
import com.manifestreader.user.service.market.UserMarketService;
import com.manifestreader.user.support.UserRequestContext;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class UserMarketServiceImpl implements UserMarketService {

    private final MarketUserFeignClient marketUserFeignClient;
    private final UserRequestContext userRequestContext;

    public UserMarketServiceImpl(MarketUserFeignClient marketUserFeignClient, UserRequestContext userRequestContext) {
        this.marketUserFeignClient = marketUserFeignClient;
        this.userRequestContext = userRequestContext;
    }

    @Override
    public FreightDemandVO createDemand(FreightDemandCreateRequest request) {
        return unwrap(marketUserFeignClient.createDemand(
                userRequestContext.currentCompanyId(),
                userRequestContext.currentUserId(),
                userRequestContext.currentTraceId(),
                request
        ));
    }

    @Override
    public PageResult<FreightDemandVO> pageDemands(FreightDemandPageQuery query) {
        return unwrap(marketUserFeignClient.pageDemands(
                userRequestContext.currentCompanyId(),
                userRequestContext.currentUserId(),
                userRequestContext.currentTraceId(),
                query.keyword(),
                query.status(),
                query.pageNo(),
                query.pageSize()
        ));
    }

    @Override
    public PageResult<FreightDemandVO> pageMineDemands(FreightDemandPageQuery query) {
        return unwrap(marketUserFeignClient.pageMineDemands(
                userRequestContext.currentCompanyId(),
                userRequestContext.currentUserId(),
                userRequestContext.currentTraceId(),
                query.keyword(),
                query.status(),
                query.pageNo(),
                query.pageSize()
        ));
    }

    @Override
    public FreightDemandDetailVO detailDemand(Long demandId) {
        return unwrap(marketUserFeignClient.detailDemand(
                userRequestContext.currentCompanyId(),
                userRequestContext.currentUserId(),
                userRequestContext.currentTraceId(),
                demandId
        ));
    }

    @Override
    public FreightDemandVO cancelDemand(Long demandId) {
        return unwrap(marketUserFeignClient.cancelDemand(
                userRequestContext.currentCompanyId(),
                userRequestContext.currentUserId(),
                userRequestContext.currentTraceId(),
                demandId
        ));
    }

    @Override
    public FreightQuoteVO submitQuote(Long demandId, FreightQuoteCreateRequest request) {
        return unwrap(marketUserFeignClient.submitQuote(
                userRequestContext.currentCompanyId(),
                userRequestContext.currentUserId(),
                userRequestContext.currentTraceId(),
                demandId,
                request
        ));
    }

    @Override
    public List<FreightQuoteVO> listQuotes(Long demandId) {
        return unwrap(marketUserFeignClient.listQuotes(
                userRequestContext.currentCompanyId(),
                userRequestContext.currentUserId(),
                userRequestContext.currentTraceId(),
                demandId
        ));
    }

    @Override
    public FreightQuoteVO withdrawQuote(Long demandId, Long quoteId) {
        return unwrap(marketUserFeignClient.withdrawQuote(
                userRequestContext.currentCompanyId(),
                userRequestContext.currentUserId(),
                userRequestContext.currentTraceId(),
                demandId,
                quoteId
        ));
    }

    @Override
    public FreightOrderVO acceptQuote(Long demandId, FreightDemandAcceptRequest request) {
        return unwrap(marketUserFeignClient.acceptQuote(
                userRequestContext.currentCompanyId(),
                userRequestContext.currentUserId(),
                userRequestContext.currentTraceId(),
                demandId,
                request
        ));
    }

    @Override
    public PageResult<FreightOrderVO> pageMyAcceptedOrders(Long pageNo, Long pageSize) {
        return unwrap(marketUserFeignClient.pageMyAcceptedOrders(
                userRequestContext.currentCompanyId(),
                userRequestContext.currentUserId(),
                userRequestContext.currentTraceId(),
                pageNo,
                pageSize
        ));
    }

    @Override
    public FreightOrderVO startOrder(Long orderId) {
        return unwrap(marketUserFeignClient.startOrder(
                userRequestContext.currentCompanyId(),
                userRequestContext.currentUserId(),
                userRequestContext.currentTraceId(),
                orderId
        ));
    }

    @Override
    public FreightOrderVO completeOrder(Long orderId) {
        return unwrap(marketUserFeignClient.completeOrder(
                userRequestContext.currentCompanyId(),
                userRequestContext.currentUserId(),
                userRequestContext.currentTraceId(),
                orderId
        ));
    }

    private <T> T unwrap(R<T> response) {
        if (response == null || !response.isSuccess()) {
            throw new BusinessException(
                    ErrorCode.INTERNAL_ERROR.getCode(),
                    response == null ? "market 服务不可用" : response.getMessage()
            );
        }
        return response.getData();
    }
}
