package com.manifestreader.admin.service.user;

import static org.assertj.core.api.Assertions.assertThat;

import com.manifestreader.admin.feign.UserFeignClient;
import com.manifestreader.admin.model.vo.AdminUserBillVO;
import com.manifestreader.common.result.PageResult;
import com.manifestreader.common.result.R;
import com.manifestreader.model.vo.BillSummaryVO;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class AdminUserServiceImplTest {

    @Test
    void listUserBillsUsesUserServiceFeignClient() {
        UserFeignClient userFeignClient = (keyword, status, pageNo, pageSize) -> {
            PageResult<BillSummaryVO> page = PageResult.empty(1, 10);
            page.setTotal(1);
            page.setRecords(List.of(new BillSummaryVO(
                    1L,
                    "MRBL-001",
                    "BK-001",
                    "COSCO / 001E",
                    "Shanghai",
                    "Los Angeles",
                    "Electronics",
                    "12 CTNS",
                    "CONFIRMED",
                    "SUCCESS",
                    LocalDateTime.now()
            )));
            return R.ok(page);
        };
        AdminUserServiceImpl service = new AdminUserServiceImpl(userFeignClient);

        List<AdminUserBillVO> bills = service.listUserBills("u-1001");

        assertThat(bills).hasSize(1);
        assertThat(bills.get(0).getBlNo()).isEqualTo("MRBL-001");
        assertThat(bills.get(0).getPol()).isEqualTo("Shanghai");
        assertThat(bills.get(0).getPod()).isEqualTo("Los Angeles");
    }
}
