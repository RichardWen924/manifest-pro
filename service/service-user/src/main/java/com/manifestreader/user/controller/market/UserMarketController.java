package com.manifestreader.user.controller.market;

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
import com.manifestreader.user.service.market.UserMarketService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "用户端-货运市场")
@RestController
@RequestMapping("/user/market")
public class UserMarketController {

    private final UserMarketService userMarketService;

    public UserMarketController(UserMarketService userMarketService) {
        this.userMarketService = userMarketService;
    }

    @PostMapping("/demands")
    public R<FreightDemandVO> createDemand(@Valid @RequestBody FreightDemandCreateRequest request) {
        return R.ok(userMarketService.createDemand(request));
    }

    @GetMapping("/demands/page")
    public R<PageResult<FreightDemandVO>> pageDemands(FreightDemandPageQuery query) {
        return R.ok(userMarketService.pageDemands(query));
    }

    @GetMapping("/demands/mine/page")
    public R<PageResult<FreightDemandVO>> pageMineDemands(FreightDemandPageQuery query) {
        return R.ok(userMarketService.pageMineDemands(query));
    }

    @GetMapping("/demands/{demandId}")
    public R<FreightDemandDetailVO> detailDemand(@PathVariable Long demandId) {
        return R.ok(userMarketService.detailDemand(demandId));
    }

    @PostMapping("/demands/{demandId}/cancel")
    public R<FreightDemandVO> cancelDemand(@PathVariable Long demandId) {
        return R.ok(userMarketService.cancelDemand(demandId));
    }

    @PostMapping("/demands/{demandId}/quotes")
    public R<FreightQuoteVO> submitQuote(
            @PathVariable Long demandId,
            @Valid @RequestBody FreightQuoteCreateRequest request) {
        return R.ok(userMarketService.submitQuote(demandId, request));
    }

    @GetMapping("/demands/{demandId}/quotes")
    public R<List<FreightQuoteVO>> listQuotes(@PathVariable Long demandId) {
        return R.ok(userMarketService.listQuotes(demandId));
    }

    @PostMapping("/demands/{demandId}/quotes/{quoteId}/withdraw")
    public R<FreightQuoteVO> withdrawQuote(@PathVariable Long demandId, @PathVariable Long quoteId) {
        return R.ok(userMarketService.withdrawQuote(demandId, quoteId));
    }

    @PostMapping("/demands/{demandId}/accept")
    public R<FreightOrderVO> acceptQuote(
            @PathVariable Long demandId,
            @Valid @RequestBody FreightDemandAcceptRequest request) {
        return R.ok(userMarketService.acceptQuote(demandId, request));
    }

    @GetMapping("/orders/mine/page")
    public R<PageResult<FreightOrderVO>> pageMyAcceptedOrders(
            @RequestParam(required = false) Long pageNo,
            @RequestParam(required = false) Long pageSize) {
        return R.ok(userMarketService.pageMyAcceptedOrders(pageNo, pageSize));
    }

    @PostMapping("/orders/{orderId}/start")
    public R<FreightOrderVO> startOrder(@PathVariable Long orderId) {
        return R.ok(userMarketService.startOrder(orderId));
    }

    @PostMapping("/orders/{orderId}/complete")
    public R<FreightOrderVO> completeOrder(@PathVariable Long orderId) {
        return R.ok(userMarketService.completeOrder(orderId));
    }
}
