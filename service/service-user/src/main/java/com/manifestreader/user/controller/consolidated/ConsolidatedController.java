package com.manifestreader.user.controller.consolidated;

import com.manifestreader.common.result.PageResult;
import com.manifestreader.common.result.R;
import com.manifestreader.user.model.dto.ConsolidatedQuery;
import com.manifestreader.user.model.vo.ConsolidatedDataVO;
import com.manifestreader.user.service.ConsolidatedService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "用户端-合并数据")
@RestController
@RequestMapping("/user/consolidated")
public class ConsolidatedController {

    private final ConsolidatedService consolidatedService;

    public ConsolidatedController(ConsolidatedService consolidatedService) {
        this.consolidatedService = consolidatedService;
    }

    @Operation(summary = "合并数据分页")
    @GetMapping("/page")
    public R<PageResult<ConsolidatedDataVO>> page(ConsolidatedQuery query) {
        return R.ok(consolidatedService.page(query));
    }

    @Operation(summary = "提单合并数据详情")
    @GetMapping("/bills/{billId}")
    public R<ConsolidatedDataVO> detail(@PathVariable Long billId) {
        return R.ok(consolidatedService.detail(billId));
    }
}
