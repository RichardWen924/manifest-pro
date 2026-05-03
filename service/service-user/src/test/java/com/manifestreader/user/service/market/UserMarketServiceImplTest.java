package com.manifestreader.user.service.market;

import static org.assertj.core.api.Assertions.assertThat;

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
import com.manifestreader.user.feign.MarketUserFeignClient;
import com.manifestreader.user.service.impl.UserMarketServiceImpl;
import com.manifestreader.user.support.UserRequestContext;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class UserMarketServiceImplTest {

    @Test
    void createDemandAndQueryMineUseMarketServiceFeignClient() {
        MarketUserFeignClient feignClient = new StubMarketUserFeignClient();
        UserRequestContext userRequestContext = new StubUserRequestContext();
        UserMarketServiceImpl service = new UserMarketServiceImpl(feignClient, userRequestContext);

        FreightDemandVO created = service.createDemand(new FreightDemandCreateRequest(
                "上海到鹿特丹整柜",
                "机械设备",
                "SHANGHAI",
                "ROTTERDAM",
                null,
                BigDecimal.TEN,
                "BOX",
                new BigDecimal("8888"),
                "CNY",
                "张三",
                "13800000000",
                "handle with care",
                List.of(10L, 11L)
        ));
        PageResult<FreightDemandVO> minePage = service.pageMineDemands(new FreightDemandPageQuery(null, null, 1L, 10L));
        PageResult<FreightOrderVO> orderPage = service.pageMyAcceptedOrders(1L, 10L);

        assertThat(created.auditStatus()).isEqualTo("PENDING");
        assertThat(minePage.getRecords()).hasSize(1);
        assertThat(orderPage.getRecords()).hasSize(1);
        assertThat(orderPage.getRecords().get(0).orderStatus()).isEqualTo("CREATED");
    }

    private static final class StubMarketUserFeignClient implements MarketUserFeignClient {

        @Override
        public R<FreightDemandVO> createDemand(Long companyId, Long userId, String traceId, FreightDemandCreateRequest request) {
            return R.ok(new FreightDemandVO(
                    1L,
                    "FD-001",
                    request.title(),
                    request.goodsName(),
                    request.departurePort(),
                    request.destinationPort(),
                    "PENDING_REVIEW",
                    "PENDING",
                    request.budgetAmount(),
                    request.currencyCode(),
                    LocalDateTime.now()
            ));
        }

        @Override
        public R<PageResult<FreightDemandVO>> pageDemands(Long companyId, Long userId, String traceId, String keyword, String status, Long pageNo, Long pageSize) {
            return R.ok(PageResult.empty(pageNo, pageSize));
        }

        @Override
        public R<PageResult<FreightDemandVO>> pageMineDemands(Long companyId, Long userId, String traceId, String keyword, String status, Long pageNo, Long pageSize) {
            PageResult<FreightDemandVO> page = PageResult.empty(pageNo, pageSize);
            page.setTotal(1);
            page.setRecords(List.of(new FreightDemandVO(
                    1L,
                    "FD-001",
                    "上海到鹿特丹整柜",
                    "机械设备",
                    "SHANGHAI",
                    "ROTTERDAM",
                    "PENDING_REVIEW",
                    "PENDING",
                    new BigDecimal("8888"),
                    "CNY",
                    LocalDateTime.now()
            )));
            return R.ok(page);
        }

        @Override
        public R<FreightDemandDetailVO> detailDemand(Long companyId, Long userId, String traceId, Long demandId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public R<FreightQuoteVO> submitQuote(Long companyId, Long userId, String traceId, Long demandId, FreightQuoteCreateRequest request) {
            throw new UnsupportedOperationException();
        }

        @Override
        public R<List<FreightQuoteVO>> listQuotes(Long companyId, Long userId, String traceId, Long demandId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public R<FreightOrderVO> acceptQuote(Long companyId, Long userId, String traceId, Long demandId, FreightDemandAcceptRequest request) {
            throw new UnsupportedOperationException();
        }

        @Override
        public R<PageResult<FreightOrderVO>> pageMyAcceptedOrders(Long companyId, Long userId, String traceId, Long pageNo, Long pageSize) {
            PageResult<FreightOrderVO> page = PageResult.empty(pageNo, pageSize);
            page.setTotal(1);
            page.setRecords(List.of(new FreightOrderVO(
                    9L,
                    "FO-001",
                    1L,
                    12L,
                    "CREATED",
                    LocalDateTime.now()
            )));
            return R.ok(page);
        }

        @Override
        public R<FreightDemandVO> cancelDemand(Long companyId, Long userId, String traceId, Long demandId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public R<FreightQuoteVO> withdrawQuote(Long companyId, Long userId, String traceId, Long demandId, Long quoteId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public R<FreightOrderVO> startOrder(Long companyId, Long userId, String traceId, Long orderId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public R<FreightOrderVO> completeOrder(Long companyId, Long userId, String traceId, Long orderId) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class StubUserRequestContext implements UserRequestContext {

        @Override
        public Long currentCompanyId() {
            return 2L;
        }

        @Override
        public Long currentUserId() {
            return 3L;
        }

        @Override
        public String currentTraceId() {
            return "trace-user-market-test";
        }
    }
}
