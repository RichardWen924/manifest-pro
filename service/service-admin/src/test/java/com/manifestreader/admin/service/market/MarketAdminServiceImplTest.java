package com.manifestreader.admin.service.market;

import static org.assertj.core.api.Assertions.assertThat;

import com.manifestreader.admin.feign.MarketAdminFeignClient;
import com.manifestreader.common.result.PageResult;
import com.manifestreader.common.result.R;
import com.manifestreader.model.dto.MarketDemandAuditRequest;
import com.manifestreader.model.vo.MarketDemandAdminVO;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class MarketAdminServiceImplTest {

    @Test
    void pagePendingReviewDemandsUsesMarketServiceFeignClient() {
        MarketAdminFeignClient feignClient = new MarketAdminFeignClient() {
            @Override
            public R<PageResult<MarketDemandAdminVO>> pageDemands(String keyword, String auditStatus, Long pageNo, Long pageSize) {
                PageResult<MarketDemandAdminVO> page = PageResult.empty(1, 10);
                page.setTotal(1);
                page.setRecords(List.of(new MarketDemandAdminVO(
                        1L,
                        "FD-001",
                        3L,
                        "上海到汉堡拼箱",
                        "家具",
                        "SHANGHAI",
                        "HAMBURG",
                        "PENDING_REVIEW",
                        "PENDING",
                        LocalDateTime.now()
                )));
                return R.ok(page);
            }

            @Override
            public R<MarketDemandAdminVO> auditDemand(Long demandId, MarketDemandAuditRequest request) {
                return R.ok(new MarketDemandAdminVO(
                        demandId,
                        "FD-001",
                        3L,
                        "上海到汉堡拼箱",
                        "家具",
                        "SHANGHAI",
                        "HAMBURG",
                        "PUBLISHED",
                        request.auditStatus(),
                        LocalDateTime.now()
                ));
            }
        };
        MarketAdminServiceImpl service = new MarketAdminServiceImpl(feignClient);

        PageResult<MarketDemandAdminVO> page = service.pageDemands(null, "PENDING", 1L, 10L);
        MarketDemandAdminVO reviewed = service.auditDemand(1L, new MarketDemandAuditRequest("APPROVED", "ok"));

        assertThat(page.getRecords()).hasSize(1);
        assertThat(page.getRecords().get(0).auditStatus()).isEqualTo("PENDING");
        assertThat(reviewed.auditStatus()).isEqualTo("APPROVED");
        assertThat(reviewed.demandStatus()).isEqualTo("PUBLISHED");
    }
}
