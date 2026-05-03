package com.manifestreader.market.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.manifestreader.market.messaging.FreightDemandAcceptedPublisher;
import com.manifestreader.market.mapper.FreightOrderMapper;
import com.manifestreader.market.mapper.FreightOrderTimelineMapper;
import com.manifestreader.market.mapper.FreightQuoteMapper;
import com.manifestreader.market.mapper.FreightDemandAttachmentMapper;
import com.manifestreader.market.mapper.FreightDemandMapper;
import com.manifestreader.market.support.UserRequestContext;
import com.manifestreader.model.dto.FreightDemandAcceptRequest;
import com.manifestreader.model.dto.FreightDemandCreateRequest;
import com.manifestreader.model.dto.FreightQuoteCreateRequest;
import com.manifestreader.model.dto.MarketDemandAuditRequest;
import com.manifestreader.model.entity.FreightDemandAttachmentEntity;
import com.manifestreader.model.entity.FreightDemandEntity;
import com.manifestreader.model.entity.FreightOrderEntity;
import com.manifestreader.model.entity.FreightQuoteEntity;
import com.manifestreader.model.vo.FreightDemandVO;
import com.manifestreader.model.vo.FreightOrderVO;
import com.manifestreader.model.vo.FreightQuoteVO;
import com.manifestreader.model.vo.MarketDemandAdminVO;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class FreightDemandServiceImplTest {

    @Mock
    private FreightDemandMapper demandMapper;

    @Mock
    private FreightDemandAttachmentMapper attachmentMapper;

    @Mock
    private FreightQuoteMapper quoteMapper;

    @Mock
    private FreightOrderMapper orderMapper;

    @Mock
    private FreightOrderTimelineMapper timelineMapper;

    @Mock
    private UserRequestContext userRequestContext;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private FreightDemandAcceptedPublisher acceptedPublisher;

    @InjectMocks
    private FreightDemandServiceImpl service;

    @Test
    void createDemandPersistsDemandAndAttachments() {
        when(userRequestContext.currentCompanyId()).thenReturn(2L);
        when(userRequestContext.currentUserId()).thenReturn(3L);
        doAnswer(invocation -> {
            FreightDemandEntity entity = invocation.getArgument(0);
            entity.setId(1001L);
            return 1;
        }).when(demandMapper).insert(any(FreightDemandEntity.class));

        FreightDemandCreateRequest request = new FreightDemandCreateRequest(
                "上海至新加坡整柜需求",
                "电子元件",
                "SHANGHAI",
                "SINGAPORE",
                LocalDate.of(2026, 5, 20),
                new BigDecimal("12"),
                "CBM",
                new BigDecimal("2800"),
                "CNY",
                "Alice",
                "13800000001",
                "需要报关配套",
                List.of(9001L, 9002L)
        );

        FreightDemandVO result = service.createDemand(request);

        assertThat(result.demandStatus()).isEqualTo("PENDING_REVIEW");
        assertThat(result.auditStatus()).isEqualTo("PENDING");
        verify(demandMapper).insert(argThat((FreightDemandEntity entity) ->
                "PENDING_REVIEW".equals(entity.getDemandStatus())
                        && "PENDING".equals(entity.getAuditStatus())
                        && "电子元件".equals(entity.getGoodsName())));
        verify(attachmentMapper, times(2)).insert(any(FreightDemandAttachmentEntity.class));
    }

    @Test
    void submitQuoteCreatesSubmittedQuote() {
        when(userRequestContext.currentCompanyId()).thenReturn(9L);
        when(userRequestContext.currentUserId()).thenReturn(10L);
        FreightDemandEntity demand = new FreightDemandEntity();
        demand.setId(1001L);
        demand.setDemandStatus("PUBLISHED");
        when(demandMapper.selectById(1001L)).thenReturn(demand);
        doAnswer(invocation -> {
            FreightQuoteEntity entity = invocation.getArgument(0);
            entity.setId(2001L);
            return 1;
        }).when(quoteMapper).insert(any(FreightQuoteEntity.class));

        FreightQuoteVO result = service.submitQuote(1001L, new FreightQuoteCreateRequest(
                new BigDecimal("2600"),
                "CNY",
                7,
                "可提供拖车"
        ));

        assertThat(result.quoteStatus()).isEqualTo("SUBMITTED");
        verify(quoteMapper).insert(argThat((FreightQuoteEntity entity) ->
                Long.valueOf(1001L).equals(entity.getDemandId())
                        && "SUBMITTED".equals(entity.getQuoteStatus())));
    }

    @Test
    void acceptQuoteCreatesOrderAndMarksDemandLocked() {
        when(userRequestContext.currentUserId()).thenReturn(3L);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent("market:demand:accept:1001", "2001", Duration.ofSeconds(10))).thenReturn(Boolean.TRUE);

        FreightDemandEntity demand = new FreightDemandEntity();
        demand.setId(1001L);
        demand.setCompanyId(2L);
        demand.setPublisherUserId(3L);
        demand.setDemandStatus("PUBLISHED");

        FreightQuoteEntity quote = new FreightQuoteEntity();
        quote.setId(2001L);
        quote.setDemandId(1001L);
        quote.setCompanyId(9L);
        quote.setQuoterUserId(10L);
        quote.setQuoteStatus("SUBMITTED");

        when(demandMapper.selectById(1001L)).thenReturn(demand);
        when(quoteMapper.selectById(2001L)).thenReturn(quote);
        doAnswer(invocation -> {
            FreightOrderEntity entity = invocation.getArgument(0);
            entity.setId(3001L);
            return 1;
        }).when(orderMapper).insert(any(FreightOrderEntity.class));

        FreightOrderVO result = service.acceptQuote(1001L, new FreightDemandAcceptRequest(2001L));

        assertThat(result.orderStatus()).isEqualTo("CREATED");
        verify(orderMapper).insert(any(FreightOrderEntity.class));
        verify(demandMapper).updateById(argThat((FreightDemandEntity entity) ->
                "LOCKED".equals(entity.getDemandStatus())
                        && Long.valueOf(2001L).equals(entity.getAcceptedQuoteId())));
        verify(acceptedPublisher).publish(any());
    }

    @Test
    void acceptQuoteSkipsWhenRedisLockAlreadyHeld() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent("market:demand:accept:1001", "2001", Duration.ofSeconds(10))).thenReturn(Boolean.FALSE);

        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                        service.acceptQuote(1001L, new FreightDemandAcceptRequest(2001L)))
                .isInstanceOf(com.manifestreader.common.exception.BusinessException.class)
                .hasMessageContaining("正在处理");
    }

    @Test
    void auditDemandApprovePublishesPendingDemand() {
        when(userRequestContext.currentUserId()).thenReturn(99L);
        FreightDemandEntity demand = new FreightDemandEntity();
        demand.setId(1001L);
        demand.setDemandStatus("PENDING_REVIEW");
        demand.setAuditStatus("PENDING");
        when(demandMapper.selectById(1001L)).thenReturn(demand);

        MarketDemandAdminVO result = service.auditDemand(1001L, new MarketDemandAuditRequest("APPROVED", "审核通过"));

        assertThat(result.demandStatus()).isEqualTo("PUBLISHED");
        assertThat(result.auditStatus()).isEqualTo("APPROVED");
        verify(demandMapper).updateById(argThat((FreightDemandEntity entity) ->
                "PUBLISHED".equals(entity.getDemandStatus())
                        && "APPROVED".equals(entity.getAuditStatus())
                        && Long.valueOf(99L).equals(entity.getAuditedBy())));
    }

    @Test
    void cancelDemandByPublisherMarksCancelledAndRejectsOpenQuotes() {
        when(userRequestContext.currentUserId()).thenReturn(3L);
        FreightDemandEntity demand = new FreightDemandEntity();
        demand.setId(1001L);
        demand.setPublisherUserId(3L);
        demand.setDemandStatus("QUOTING");
        when(demandMapper.selectById(1001L)).thenReturn(demand);

        FreightQuoteEntity openQuote = new FreightQuoteEntity();
        openQuote.setId(2001L);
        openQuote.setDemandId(1001L);
        openQuote.setQuoteStatus("SUBMITTED");
        FreightQuoteEntity withdrawnQuote = new FreightQuoteEntity();
        withdrawnQuote.setId(2002L);
        withdrawnQuote.setDemandId(1001L);
        withdrawnQuote.setQuoteStatus("WITHDRAWN");
        when(quoteMapper.selectList(any())).thenReturn(List.of(openQuote, withdrawnQuote));

        FreightDemandVO result = service.cancelDemand(1001L);

        assertThat(result.demandStatus()).isEqualTo("CANCELLED");
        verify(demandMapper).updateById(argThat((FreightDemandEntity entity) ->
                "CANCELLED".equals(entity.getDemandStatus())
                        && Long.valueOf(3L).equals(entity.getUpdatedBy())));
        verify(quoteMapper).updateById(argThat((FreightQuoteEntity entity) ->
                Long.valueOf(2001L).equals(entity.getId())
                        && "REJECTED".equals(entity.getQuoteStatus())));
        verify(quoteMapper, never()).updateById(argThat((FreightQuoteEntity entity) ->
                Long.valueOf(2002L).equals(entity.getId())));
    }

    @Test
    void withdrawQuoteByOwnerMarksWithdrawnAndReopensDemandWhenNoOpenQuotesRemain() {
        when(userRequestContext.currentUserId()).thenReturn(10L);
        FreightDemandEntity demand = new FreightDemandEntity();
        demand.setId(1001L);
        demand.setPublisherUserId(3L);
        demand.setDemandStatus("QUOTING");
        when(demandMapper.selectById(1001L)).thenReturn(demand);

        FreightQuoteEntity quote = new FreightQuoteEntity();
        quote.setId(2001L);
        quote.setDemandId(1001L);
        quote.setQuoterUserId(10L);
        quote.setQuoteStatus("SUBMITTED");
        when(quoteMapper.selectById(2001L)).thenReturn(quote);
        when(quoteMapper.selectList(any())).thenReturn(List.of(quote));

        FreightQuoteVO result = service.withdrawQuote(1001L, 2001L);

        assertThat(result.quoteStatus()).isEqualTo("WITHDRAWN");
        verify(quoteMapper).updateById(argThat((FreightQuoteEntity entity) ->
                Long.valueOf(2001L).equals(entity.getId())
                        && "WITHDRAWN".equals(entity.getQuoteStatus())));
        verify(demandMapper).updateById(argThat((FreightDemandEntity entity) ->
                Long.valueOf(1001L).equals(entity.getId())
                        && "PUBLISHED".equals(entity.getDemandStatus())));
    }

    @Test
    void startOrderByAgentMarksOrderInProgressAndDemandFulfilling() {
        when(userRequestContext.currentUserId()).thenReturn(10L);
        FreightOrderEntity order = new FreightOrderEntity();
        order.setId(3001L);
        order.setDemandId(1001L);
        order.setAgentUserId(10L);
        order.setPublisherUserId(3L);
        order.setOrderStatus("CREATED");
        when(orderMapper.selectById(3001L)).thenReturn(order);

        FreightDemandEntity demand = new FreightDemandEntity();
        demand.setId(1001L);
        demand.setDemandStatus("LOCKED");
        when(demandMapper.selectById(1001L)).thenReturn(demand);

        FreightOrderVO result = service.startOrder(3001L);

        assertThat(result.orderStatus()).isEqualTo("IN_PROGRESS");
        verify(orderMapper).updateById(argThat((FreightOrderEntity entity) ->
                Long.valueOf(3001L).equals(entity.getId())
                        && "IN_PROGRESS".equals(entity.getOrderStatus())));
        verify(demandMapper).updateById(argThat((FreightDemandEntity entity) ->
                Long.valueOf(1001L).equals(entity.getId())
                        && "FULFILLING".equals(entity.getDemandStatus())));
        verify(timelineMapper).insert(argThat((com.manifestreader.model.entity.FreightOrderTimelineEntity entity) ->
                Long.valueOf(3001L).equals(entity.getOrderId())
                        && "ORDER_STARTED".equals(entity.getEventType())));
    }

    @Test
    void completeOrderByPublisherMarksOrderAndDemandCompleted() {
        when(userRequestContext.currentUserId()).thenReturn(3L);
        FreightOrderEntity order = new FreightOrderEntity();
        order.setId(3001L);
        order.setDemandId(1001L);
        order.setAgentUserId(10L);
        order.setPublisherUserId(3L);
        order.setOrderStatus("IN_PROGRESS");
        when(orderMapper.selectById(3001L)).thenReturn(order);

        FreightDemandEntity demand = new FreightDemandEntity();
        demand.setId(1001L);
        demand.setDemandStatus("FULFILLING");
        when(demandMapper.selectById(1001L)).thenReturn(demand);

        FreightOrderVO result = service.completeOrder(3001L);

        assertThat(result.orderStatus()).isEqualTo("COMPLETED");
        verify(orderMapper).updateById(argThat((FreightOrderEntity entity) ->
                Long.valueOf(3001L).equals(entity.getId())
                        && "COMPLETED".equals(entity.getOrderStatus())));
        verify(demandMapper).updateById(argThat((FreightDemandEntity entity) ->
                Long.valueOf(1001L).equals(entity.getId())
                        && "COMPLETED".equals(entity.getDemandStatus())));
        verify(timelineMapper).insert(argThat((com.manifestreader.model.entity.FreightOrderTimelineEntity entity) ->
                Long.valueOf(3001L).equals(entity.getOrderId())
                        && "ORDER_COMPLETED".equals(entity.getEventType())));
    }
}
