package com.manifestreader.market.controller;

import com.manifestreader.common.result.PageResult;
import com.manifestreader.common.result.R;
import com.manifestreader.market.service.FreightDemandService;
import com.manifestreader.model.vo.FreightOrderVO;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "货运市场-订单")
@RestController
@RequestMapping("/market/orders")
public class MarketOrderController {

    private final FreightDemandService freightDemandService;

    public MarketOrderController(FreightDemandService freightDemandService) {
        this.freightDemandService = freightDemandService;
    }

    @GetMapping("/mine/page")
    public R<PageResult<FreightOrderVO>> pageMyAcceptedOrders(
            @RequestParam(required = false) Long pageNo,
            @RequestParam(required = false) Long pageSize) {
        return R.ok(freightDemandService.pageMyAcceptedOrders(pageNo, pageSize));
    }

    @PostMapping("/{orderId}/start")
    public R<FreightOrderVO> startOrder(@PathVariable Long orderId) {
        return R.ok(freightDemandService.startOrder(orderId));
    }

    @PostMapping("/{orderId}/complete")
    public R<FreightOrderVO> completeOrder(@PathVariable Long orderId) {
        return R.ok(freightDemandService.completeOrder(orderId));
    }
}
