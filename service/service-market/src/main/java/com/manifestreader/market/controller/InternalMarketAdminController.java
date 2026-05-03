package com.manifestreader.market.controller;

import com.manifestreader.common.result.PageResult;
import com.manifestreader.common.result.R;
import com.manifestreader.market.service.FreightDemandService;
import com.manifestreader.model.dto.MarketDemandAuditRequest;
import com.manifestreader.model.vo.MarketDemandAdminVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/market/admin/demands")
public class InternalMarketAdminController {

    private final FreightDemandService freightDemandService;

    public InternalMarketAdminController(FreightDemandService freightDemandService) {
        this.freightDemandService = freightDemandService;
    }

    @GetMapping("/page")
    public R<PageResult<MarketDemandAdminVO>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String auditStatus,
            @RequestParam(required = false) Long pageNo,
            @RequestParam(required = false) Long pageSize) {
        return R.ok(freightDemandService.pageAdminDemands(keyword, auditStatus, pageNo, pageSize));
    }

    @PostMapping("/{id}/audit")
    public R<MarketDemandAdminVO> audit(@PathVariable Long id, @Valid @RequestBody MarketDemandAuditRequest request) {
        return R.ok(freightDemandService.auditDemand(id, request));
    }
}
