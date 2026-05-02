package com.manifestreader.admin.feign;

import com.manifestreader.common.result.PageResult;
import com.manifestreader.common.result.R;
import com.manifestreader.model.vo.BillSummaryVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "manifest-reader-user", contextId = "adminUserFeignClient")
public interface UserFeignClient {

    @GetMapping("/internal/user/bills/page")
    R<PageResult<BillSummaryVO>> pageBills(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "pageNo", required = false) Long pageNo,
            @RequestParam(value = "pageSize", required = false) Long pageSize
    );
}
