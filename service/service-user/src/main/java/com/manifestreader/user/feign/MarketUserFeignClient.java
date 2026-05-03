package com.manifestreader.user.feign;

import com.manifestreader.common.constant.HeaderConstants;
import com.manifestreader.common.result.PageResult;
import com.manifestreader.common.result.R;
import com.manifestreader.model.dto.FreightDemandAcceptRequest;
import com.manifestreader.model.dto.FreightDemandCreateRequest;
import com.manifestreader.model.dto.FreightQuoteCreateRequest;
import com.manifestreader.model.vo.FreightDemandDetailVO;
import com.manifestreader.model.vo.FreightDemandVO;
import com.manifestreader.model.vo.FreightOrderVO;
import com.manifestreader.model.vo.FreightQuoteVO;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "manifest-reader-market", contextId = "userMarketFeignClient")
public interface MarketUserFeignClient {

    @PostMapping("/market/demands")
    R<FreightDemandVO> createDemand(
            @RequestHeader(HeaderConstants.COMPANY_ID) Long companyId,
            @RequestHeader(HeaderConstants.USER_ID) Long userId,
            @RequestHeader(HeaderConstants.TRACE_ID) String traceId,
            @RequestBody FreightDemandCreateRequest request
    );

    @GetMapping("/market/demands/page")
    R<PageResult<FreightDemandVO>> pageDemands(
            @RequestHeader(HeaderConstants.COMPANY_ID) Long companyId,
            @RequestHeader(HeaderConstants.USER_ID) Long userId,
            @RequestHeader(HeaderConstants.TRACE_ID) String traceId,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "pageNo", required = false) Long pageNo,
            @RequestParam(value = "pageSize", required = false) Long pageSize
    );

    @GetMapping("/market/demands/mine/page")
    R<PageResult<FreightDemandVO>> pageMineDemands(
            @RequestHeader(HeaderConstants.COMPANY_ID) Long companyId,
            @RequestHeader(HeaderConstants.USER_ID) Long userId,
            @RequestHeader(HeaderConstants.TRACE_ID) String traceId,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "pageNo", required = false) Long pageNo,
            @RequestParam(value = "pageSize", required = false) Long pageSize
    );

    @GetMapping("/market/demands/{demandId}")
    R<FreightDemandDetailVO> detailDemand(
            @RequestHeader(HeaderConstants.COMPANY_ID) Long companyId,
            @RequestHeader(HeaderConstants.USER_ID) Long userId,
            @RequestHeader(HeaderConstants.TRACE_ID) String traceId,
            @PathVariable("demandId") Long demandId
    );

    @PostMapping("/market/demands/{demandId}/cancel")
    R<FreightDemandVO> cancelDemand(
            @RequestHeader(HeaderConstants.COMPANY_ID) Long companyId,
            @RequestHeader(HeaderConstants.USER_ID) Long userId,
            @RequestHeader(HeaderConstants.TRACE_ID) String traceId,
            @PathVariable("demandId") Long demandId
    );

    @PostMapping("/market/demands/{demandId}/quotes")
    R<FreightQuoteVO> submitQuote(
            @RequestHeader(HeaderConstants.COMPANY_ID) Long companyId,
            @RequestHeader(HeaderConstants.USER_ID) Long userId,
            @RequestHeader(HeaderConstants.TRACE_ID) String traceId,
            @PathVariable("demandId") Long demandId,
            @RequestBody FreightQuoteCreateRequest request
    );

    @GetMapping("/market/demands/{demandId}/quotes")
    R<List<FreightQuoteVO>> listQuotes(
            @RequestHeader(HeaderConstants.COMPANY_ID) Long companyId,
            @RequestHeader(HeaderConstants.USER_ID) Long userId,
            @RequestHeader(HeaderConstants.TRACE_ID) String traceId,
            @PathVariable("demandId") Long demandId
    );

    @PostMapping("/market/demands/{demandId}/quotes/{quoteId}/withdraw")
    R<FreightQuoteVO> withdrawQuote(
            @RequestHeader(HeaderConstants.COMPANY_ID) Long companyId,
            @RequestHeader(HeaderConstants.USER_ID) Long userId,
            @RequestHeader(HeaderConstants.TRACE_ID) String traceId,
            @PathVariable("demandId") Long demandId,
            @PathVariable("quoteId") Long quoteId
    );

    @PostMapping("/market/demands/{demandId}/accept")
    R<FreightOrderVO> acceptQuote(
            @RequestHeader(HeaderConstants.COMPANY_ID) Long companyId,
            @RequestHeader(HeaderConstants.USER_ID) Long userId,
            @RequestHeader(HeaderConstants.TRACE_ID) String traceId,
            @PathVariable("demandId") Long demandId,
            @RequestBody FreightDemandAcceptRequest request
    );

    @GetMapping("/market/orders/mine/page")
    R<PageResult<FreightOrderVO>> pageMyAcceptedOrders(
            @RequestHeader(HeaderConstants.COMPANY_ID) Long companyId,
            @RequestHeader(HeaderConstants.USER_ID) Long userId,
            @RequestHeader(HeaderConstants.TRACE_ID) String traceId,
            @RequestParam(value = "pageNo", required = false) Long pageNo,
            @RequestParam(value = "pageSize", required = false) Long pageSize
    );

    @PostMapping("/market/orders/{orderId}/start")
    R<FreightOrderVO> startOrder(
            @RequestHeader(HeaderConstants.COMPANY_ID) Long companyId,
            @RequestHeader(HeaderConstants.USER_ID) Long userId,
            @RequestHeader(HeaderConstants.TRACE_ID) String traceId,
            @PathVariable("orderId") Long orderId
    );

    @PostMapping("/market/orders/{orderId}/complete")
    R<FreightOrderVO> completeOrder(
            @RequestHeader(HeaderConstants.COMPANY_ID) Long companyId,
            @RequestHeader(HeaderConstants.USER_ID) Long userId,
            @RequestHeader(HeaderConstants.TRACE_ID) String traceId,
            @PathVariable("orderId") Long orderId
    );
}
