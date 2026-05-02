package com.manifestreader.user.controller.internal;

import com.manifestreader.common.result.PageResult;
import com.manifestreader.common.result.R;
import com.manifestreader.model.vo.BillSummaryVO;
import com.manifestreader.user.model.dto.BillPageQuery;
import com.manifestreader.user.model.vo.BillVO;
import com.manifestreader.user.service.BillService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/user/bills")
public class InternalUserBillController {

    private final BillService billService;

    public InternalUserBillController(BillService billService) {
        this.billService = billService;
    }

    @GetMapping("/page")
    public R<PageResult<BillSummaryVO>> pageBills(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "pageNo", required = false) Long pageNo,
            @RequestParam(value = "pageSize", required = false) Long pageSize) {
        PageResult<BillVO> page = billService.page(new BillPageQuery(keyword, status, pageNo, pageSize));
        PageResult<BillSummaryVO> result = PageResult.empty(page.getCurrent(), page.getSize());
        result.setTotal(page.getTotal());
        result.setRecords(page.getRecords().stream()
                .map(this::toSummary)
                .toList());
        return R.ok(result);
    }

    private BillSummaryVO toSummary(BillVO bill) {
        return new BillSummaryVO(
                bill.id(),
                bill.blNo(),
                bill.bookingNo(),
                bill.vesselVoyage(),
                bill.portOfLoading(),
                bill.portOfDischarge(),
                bill.goodsName(),
                bill.quantity(),
                bill.status(),
                bill.parseStatus(),
                bill.createdAt()
        );
    }
}
