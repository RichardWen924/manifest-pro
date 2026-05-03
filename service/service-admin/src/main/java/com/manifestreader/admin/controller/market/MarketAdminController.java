package com.manifestreader.admin.controller.market;

import com.manifestreader.admin.service.market.MarketAdminService;
import com.manifestreader.common.result.PageResult;
import com.manifestreader.common.result.Result;
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
@RequestMapping("/market/demands")
public class MarketAdminController {

    private final MarketAdminService marketAdminService;

    public MarketAdminController(MarketAdminService marketAdminService) {
        this.marketAdminService = marketAdminService;
    }

    @GetMapping("/review/page")
    public Result<PageResult<MarketDemandAdminVO>> pageDemands(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String auditStatus,
            @RequestParam(required = false) Long pageNo,
            @RequestParam(required = false) Long pageSize) {
        return Result.success(marketAdminService.pageDemands(keyword, auditStatus, pageNo, pageSize));
    }

    @PostMapping("/{demandId}/audit")
    public Result<MarketDemandAdminVO> auditDemand(
            @PathVariable Long demandId,
            @Valid @RequestBody MarketDemandAuditRequest request) {
        return Result.success(marketAdminService.auditDemand(demandId, request));
    }
}
