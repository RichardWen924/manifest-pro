package com.manifestreader.admin.feign;

import com.manifestreader.common.result.PageResult;
import com.manifestreader.common.result.R;
import com.manifestreader.model.dto.MarketDemandAuditRequest;
import com.manifestreader.model.vo.MarketDemandAdminVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "manifest-reader-market", contextId = "adminMarketFeignClient")
public interface MarketAdminFeignClient {

    @GetMapping("/internal/market/admin/demands/page")
    R<PageResult<MarketDemandAdminVO>> pageDemands(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "auditStatus", required = false) String auditStatus,
            @RequestParam(value = "pageNo", required = false) Long pageNo,
            @RequestParam(value = "pageSize", required = false) Long pageSize
    );

    @PostMapping("/internal/market/admin/demands/{demandId}/audit")
    R<MarketDemandAdminVO> auditDemand(
            @PathVariable("demandId") Long demandId,
            @RequestBody MarketDemandAuditRequest request
    );
}
