package com.manifestreader.market.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.manifestreader.common.exception.BusinessException;
import com.manifestreader.common.exception.ErrorCode;
import com.manifestreader.common.result.PageResult;
import com.manifestreader.market.messaging.FreightDemandAcceptedMessage;
import com.manifestreader.market.messaging.FreightDemandAcceptedPublisher;
import com.manifestreader.market.mapper.FreightDemandAttachmentMapper;
import com.manifestreader.market.mapper.FreightDemandMapper;
import com.manifestreader.market.mapper.FreightOrderMapper;
import com.manifestreader.market.mapper.FreightOrderTimelineMapper;
import com.manifestreader.market.mapper.FreightQuoteMapper;
import com.manifestreader.market.service.FreightDemandService;
import com.manifestreader.market.support.UserRequestContext;
import com.manifestreader.model.dto.FreightDemandAcceptRequest;
import com.manifestreader.model.dto.FreightDemandCreateRequest;
import com.manifestreader.model.dto.FreightDemandPageQuery;
import com.manifestreader.model.dto.FreightQuoteCreateRequest;
import com.manifestreader.model.dto.MarketDemandAuditRequest;
import com.manifestreader.model.entity.FreightDemandAttachmentEntity;
import com.manifestreader.model.entity.FreightDemandEntity;
import com.manifestreader.model.entity.FreightOrderEntity;
import com.manifestreader.model.entity.FreightOrderTimelineEntity;
import com.manifestreader.model.entity.FreightQuoteEntity;
import com.manifestreader.model.vo.FreightDemandDetailVO;
import com.manifestreader.model.vo.FreightDemandVO;
import com.manifestreader.model.vo.FreightOrderVO;
import com.manifestreader.model.vo.FreightQuoteVO;
import com.manifestreader.model.vo.MarketDemandAdminVO;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class FreightDemandServiceImpl implements FreightDemandService {

    private final FreightDemandMapper demandMapper;
    private final FreightDemandAttachmentMapper attachmentMapper;
    private final FreightQuoteMapper quoteMapper;
    private final FreightOrderMapper orderMapper;
    private final FreightOrderTimelineMapper timelineMapper;
    private final UserRequestContext userRequestContext;
    private final StringRedisTemplate stringRedisTemplate;
    private final FreightDemandAcceptedPublisher acceptedPublisher;

    public FreightDemandServiceImpl(
            FreightDemandMapper demandMapper,
            FreightDemandAttachmentMapper attachmentMapper,
            FreightQuoteMapper quoteMapper,
            FreightOrderMapper orderMapper,
            FreightOrderTimelineMapper timelineMapper,
            UserRequestContext userRequestContext,
            StringRedisTemplate stringRedisTemplate,
            FreightDemandAcceptedPublisher acceptedPublisher
    ) {
        this.demandMapper = demandMapper;
        this.attachmentMapper = attachmentMapper;
        this.quoteMapper = quoteMapper;
        this.orderMapper = orderMapper;
        this.timelineMapper = timelineMapper;
        this.userRequestContext = userRequestContext;
        this.stringRedisTemplate = stringRedisTemplate;
        this.acceptedPublisher = acceptedPublisher;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FreightDemandVO createDemand(FreightDemandCreateRequest request) {
        FreightDemandEntity entity = new FreightDemandEntity();
        entity.setCompanyId(userRequestContext.currentCompanyId());
        entity.setPublisherUserId(userRequestContext.currentUserId());
        entity.setDemandNo(buildDemandNo());
        entity.setTitle(request.title());
        entity.setGoodsName(request.goodsName());
        entity.setDeparturePort(request.departurePort());
        entity.setDestinationPort(request.destinationPort());
        entity.setExpectedShippingDate(request.expectedShippingDate());
        entity.setQuantity(request.quantity());
        entity.setQuantityUnit(request.quantityUnit());
        entity.setBudgetAmount(request.budgetAmount());
        entity.setCurrencyCode(StringUtils.hasText(request.currencyCode()) ? request.currencyCode() : "CNY");
        entity.setContactName(request.contactName());
        entity.setContactPhone(request.contactPhone());
        entity.setRemark(request.remark());
        entity.setDemandStatus("PENDING_REVIEW");
        entity.setAuditStatus("PENDING");
        entity.setHotScore(0L);
        entity.setDeleted(0);
        entity.setCreatedBy(userRequestContext.currentUserId());
        entity.setUpdatedBy(userRequestContext.currentUserId());
        demandMapper.insert(entity);
        saveAttachments(entity.getId(), request.attachmentFileIds());
        return toVO(entity);
    }

    @Override
    public PageResult<FreightDemandVO> page(FreightDemandPageQuery query) {
        Page<FreightDemandEntity> page = demandMapper.selectPage(
                Page.of(query.pageNo(), query.pageSize()),
                buildPublicPageWrapper(query)
        );
        PageResult<FreightDemandVO> result = PageResult.empty(page.getCurrent(), page.getSize());
        result.setTotal(page.getTotal());
        result.setRecords(page.getRecords().stream().map(this::toVO).toList());
        return result;
    }

    @Override
    public PageResult<FreightDemandVO> pageMineDemands(FreightDemandPageQuery query) {
        Page<FreightDemandEntity> page = demandMapper.selectPage(
                Page.of(query.pageNo(), query.pageSize()),
                buildMinePageWrapper(query)
        );
        PageResult<FreightDemandVO> result = PageResult.empty(page.getCurrent(), page.getSize());
        result.setTotal(page.getTotal());
        result.setRecords(page.getRecords().stream().map(this::toVO).toList());
        return result;
    }

    @Override
    public FreightDemandDetailVO detail(Long id) {
        FreightDemandEntity entity = requireDemand(id);
        List<Long> attachmentIds = attachmentMapper.selectList(new LambdaQueryWrapper<FreightDemandAttachmentEntity>()
                        .eq(FreightDemandAttachmentEntity::getDemandId, id)
                        .eq(FreightDemandAttachmentEntity::getDeleted, 0)
                        .orderByAsc(FreightDemandAttachmentEntity::getSortNo))
                .stream()
                .map(FreightDemandAttachmentEntity::getFileAssetId)
                .toList();
        return new FreightDemandDetailVO(
                entity.getId(),
                entity.getDemandNo(),
                entity.getTitle(),
                entity.getGoodsName(),
                entity.getDeparturePort(),
                entity.getDestinationPort(),
                entity.getExpectedShippingDate(),
                entity.getQuantity(),
                entity.getQuantityUnit(),
                entity.getBudgetAmount(),
                entity.getCurrencyCode(),
                entity.getContactName(),
                entity.getContactPhone(),
                entity.getRemark(),
                entity.getDemandStatus(),
                entity.getAuditStatus(),
                entity.getAcceptedQuoteId(),
                entity.getAcceptedOrderId(),
                attachmentIds,
                entity.getCreatedAt()
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FreightDemandVO cancelDemand(Long demandId) {
        FreightDemandEntity demand = requireDemand(demandId);
        assertPublisher(demand);
        if (!List.of("PENDING_REVIEW", "PUBLISHED", "QUOTING", "REJECTED").contains(demand.getDemandStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "当前需求不允许取消");
        }
        demand.setDemandStatus("CANCELLED");
        demand.setUpdatedBy(userRequestContext.currentUserId());
        demandMapper.updateById(demand);
        rejectOpenQuotes(demandId);
        return toVO(demand);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FreightQuoteVO submitQuote(Long demandId, FreightQuoteCreateRequest request) {
        FreightDemandEntity demand = requireDemand(demandId);
        if (!List.of("PUBLISHED", "QUOTING").contains(demand.getDemandStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "当前需求不允许报价");
        }
        FreightQuoteEntity entity = new FreightQuoteEntity();
        entity.setDemandId(demandId);
        entity.setCompanyId(userRequestContext.currentCompanyId());
        entity.setQuoterUserId(userRequestContext.currentUserId());
        entity.setQuoteNo(buildQuoteNo());
        entity.setPriceAmount(request.priceAmount());
        entity.setCurrencyCode(request.currencyCode());
        entity.setEstimatedDays(request.estimatedDays());
        entity.setServiceNote(request.serviceNote());
        entity.setQuoteStatus("SUBMITTED");
        entity.setDeleted(0);
        entity.setCreatedBy(userRequestContext.currentUserId());
        entity.setUpdatedBy(userRequestContext.currentUserId());
        quoteMapper.insert(entity);
        if ("PUBLISHED".equals(demand.getDemandStatus())) {
            demand.setDemandStatus("QUOTING");
            demand.setUpdatedBy(userRequestContext.currentUserId());
            demandMapper.updateById(demand);
        }
        return toQuoteVO(entity);
    }

    @Override
    public List<FreightQuoteVO> listQuotes(Long demandId) {
        return quoteMapper.selectList(new LambdaQueryWrapper<FreightQuoteEntity>()
                        .eq(FreightQuoteEntity::getDemandId, demandId)
                        .eq(FreightQuoteEntity::getDeleted, 0)
                        .orderByDesc(FreightQuoteEntity::getCreatedAt))
                .stream()
                .map(this::toQuoteVO)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FreightQuoteVO withdrawQuote(Long demandId, Long quoteId) {
        FreightDemandEntity demand = requireDemand(demandId);
        FreightQuoteEntity quote = requireExistingQuote(quoteId, demandId);
        assertQuoter(quote);
        if (!"SUBMITTED".equals(quote.getQuoteStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "当前报价不允许撤回");
        }
        quote.setQuoteStatus("WITHDRAWN");
        quote.setUpdatedBy(userRequestContext.currentUserId());
        quoteMapper.updateById(quote);
        if ("QUOTING".equals(demand.getDemandStatus()) && !hasOpenQuotes(demandId, quoteId)) {
            demand.setDemandStatus("PUBLISHED");
            demand.setUpdatedBy(userRequestContext.currentUserId());
            demandMapper.updateById(demand);
        }
        return toQuoteVO(quote);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FreightOrderVO acceptQuote(Long demandId, FreightDemandAcceptRequest request) {
        String acceptLockKey = "market:demand:accept:" + demandId;
        Boolean locked = stringRedisTemplate.opsForValue().setIfAbsent(
                acceptLockKey,
                String.valueOf(request.quoteId()),
                Duration.ofSeconds(10)
        );
        if (!Boolean.TRUE.equals(locked)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "当前需求正在处理中，请稍后重试");
        }
        try {
            FreightDemandEntity demand = requireDemand(demandId);
            FreightQuoteEntity quote = requireQuote(request.quoteId(), demandId);
            FreightOrderEntity order = new FreightOrderEntity();
            order.setOrderNo(buildOrderNo());
            order.setDemandId(demandId);
            order.setAcceptedQuoteId(quote.getId());
            order.setPublisherCompanyId(demand.getCompanyId());
            order.setPublisherUserId(demand.getPublisherUserId());
            order.setAgentCompanyId(quote.getCompanyId());
            order.setAgentUserId(quote.getQuoterUserId());
            order.setOrderStatus("CREATED");
            order.setDeleted(0);
            order.setCreatedBy(userRequestContext.currentUserId());
            order.setUpdatedBy(userRequestContext.currentUserId());
            orderMapper.insert(order);

            demand.setDemandStatus("LOCKED");
            demand.setAcceptedQuoteId(quote.getId());
            demand.setAcceptedOrderId(order.getId());
            demand.setUpdatedBy(userRequestContext.currentUserId());
            demandMapper.updateById(demand);

            quote.setQuoteStatus("ACCEPTED");
            quote.setUpdatedBy(userRequestContext.currentUserId());
            quoteMapper.updateById(quote);
            rejectOtherQuotes(demandId, quote.getId());
            acceptedPublisher.publish(buildAcceptedMessage(demandId, quote.getId(), order.getId()));
            return toOrderVO(order);
        } finally {
            stringRedisTemplate.delete(acceptLockKey);
        }
    }

    @Override
    public PageResult<FreightOrderVO> pageMyAcceptedOrders(Long pageNo, Long pageSize) {
        Page<FreightOrderEntity> page = orderMapper.selectPage(
                Page.of(normalizePageNo(pageNo), normalizePageSize(pageSize)),
                new LambdaQueryWrapper<FreightOrderEntity>()
                        .eq(FreightOrderEntity::getDeleted, 0)
                        .eq(FreightOrderEntity::getAgentUserId, userRequestContext.currentUserId())
                        .orderByDesc(FreightOrderEntity::getCreatedAt)
        );
        PageResult<FreightOrderVO> result = PageResult.empty(page.getCurrent(), page.getSize());
        result.setTotal(page.getTotal());
        result.setRecords(page.getRecords().stream().map(this::toOrderVO).toList());
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FreightOrderVO startOrder(Long orderId) {
        FreightOrderEntity order = requireOrder(orderId);
        if (!userRequestContext.currentUserId().equals(order.getAgentUserId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN.getCode(), "仅接单方可开始履约");
        }
        if (!"CREATED".equals(order.getOrderStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "当前订单不允许开始履约");
        }
        order.setOrderStatus("IN_PROGRESS");
        order.setUpdatedBy(userRequestContext.currentUserId());
        orderMapper.updateById(order);

        FreightDemandEntity demand = requireDemand(order.getDemandId());
        if ("LOCKED".equals(demand.getDemandStatus())) {
            demand.setDemandStatus("FULFILLING");
            demand.setUpdatedBy(userRequestContext.currentUserId());
            demandMapper.updateById(demand);
        }
        insertTimeline(order.getId(), "ORDER_STARTED", "货运订单已开工，代理开始履约");
        return toOrderVO(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FreightOrderVO completeOrder(Long orderId) {
        FreightOrderEntity order = requireOrder(orderId);
        if (!userRequestContext.currentUserId().equals(order.getPublisherUserId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN.getCode(), "仅发布方可完结订单");
        }
        if (!"IN_PROGRESS".equals(order.getOrderStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "当前订单不允许完结");
        }
        order.setOrderStatus("COMPLETED");
        order.setUpdatedBy(userRequestContext.currentUserId());
        orderMapper.updateById(order);

        FreightDemandEntity demand = requireDemand(order.getDemandId());
        demand.setDemandStatus("COMPLETED");
        demand.setUpdatedBy(userRequestContext.currentUserId());
        demandMapper.updateById(demand);
        insertTimeline(order.getId(), "ORDER_COMPLETED", "货运订单已完结");
        return toOrderVO(order);
    }

    @Override
    public PageResult<MarketDemandAdminVO> pageAdminDemands(String keyword, String auditStatus, Long pageNo, Long pageSize) {
        LambdaQueryWrapper<FreightDemandEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FreightDemandEntity::getDeleted, 0);
        if (StringUtils.hasText(auditStatus)) {
            wrapper.eq(FreightDemandEntity::getAuditStatus, auditStatus);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(FreightDemandEntity::getTitle, keyword)
                    .or()
                    .like(FreightDemandEntity::getGoodsName, keyword)
                    .or()
                    .like(FreightDemandEntity::getDeparturePort, keyword)
                    .or()
                    .like(FreightDemandEntity::getDestinationPort, keyword));
        }
        wrapper.orderByDesc(FreightDemandEntity::getCreatedAt);
        Page<FreightDemandEntity> page = demandMapper.selectPage(Page.of(normalizePageNo(pageNo), normalizePageSize(pageSize)), wrapper);
        PageResult<MarketDemandAdminVO> result = PageResult.empty(page.getCurrent(), page.getSize());
        result.setTotal(page.getTotal());
        result.setRecords(page.getRecords().stream().map(this::toAdminVO).toList());
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MarketDemandAdminVO auditDemand(Long demandId, MarketDemandAuditRequest request) {
        FreightDemandEntity demand = requireDemand(demandId);
        if (!List.of("APPROVED", "REJECTED").contains(request.auditStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "审核状态不合法");
        }
        demand.setAuditStatus(request.auditStatus());
        demand.setAuditRemark(request.auditRemark());
        demand.setAuditedBy(userRequestContext.currentUserId());
        demand.setAuditedAt(LocalDateTime.now());
        demand.setUpdatedBy(userRequestContext.currentUserId());
        demand.setDemandStatus("APPROVED".equals(request.auditStatus()) ? "PUBLISHED" : "REJECTED");
        demandMapper.updateById(demand);
        return toAdminVO(demand);
    }

    private LambdaQueryWrapper<FreightDemandEntity> buildPublicPageWrapper(FreightDemandPageQuery query) {
        LambdaQueryWrapper<FreightDemandEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FreightDemandEntity::getDeleted, 0);
        wrapper.eq(FreightDemandEntity::getAuditStatus, "APPROVED");
        if (StringUtils.hasText(query.status())) {
            wrapper.eq(FreightDemandEntity::getDemandStatus, query.status());
        }
        if (StringUtils.hasText(query.keyword())) {
            wrapper.and(w -> w.like(FreightDemandEntity::getTitle, query.keyword())
                    .or()
                    .like(FreightDemandEntity::getGoodsName, query.keyword())
                    .or()
                    .like(FreightDemandEntity::getDeparturePort, query.keyword())
                    .or()
                    .like(FreightDemandEntity::getDestinationPort, query.keyword()));
        }
        wrapper.orderByDesc(FreightDemandEntity::getCreatedAt);
        return wrapper;
    }

    private LambdaQueryWrapper<FreightDemandEntity> buildMinePageWrapper(FreightDemandPageQuery query) {
        LambdaQueryWrapper<FreightDemandEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FreightDemandEntity::getDeleted, 0);
        wrapper.eq(FreightDemandEntity::getPublisherUserId, userRequestContext.currentUserId());
        if (StringUtils.hasText(query.status())) {
            wrapper.eq(FreightDemandEntity::getDemandStatus, query.status());
        }
        if (StringUtils.hasText(query.keyword())) {
            wrapper.and(w -> w.like(FreightDemandEntity::getTitle, query.keyword())
                    .or()
                    .like(FreightDemandEntity::getGoodsName, query.keyword())
                    .or()
                    .like(FreightDemandEntity::getDeparturePort, query.keyword())
                    .or()
                    .like(FreightDemandEntity::getDestinationPort, query.keyword()));
        }
        wrapper.orderByDesc(FreightDemandEntity::getCreatedAt);
        return wrapper;
    }

    private void saveAttachments(Long demandId, List<Long> attachmentFileIds) {
        if (attachmentFileIds == null || attachmentFileIds.isEmpty()) {
            return;
        }
        List<Long> distinctFileIds = new ArrayList<>();
        for (Long fileId : attachmentFileIds) {
            if (fileId != null && !distinctFileIds.contains(fileId)) {
                distinctFileIds.add(fileId);
            }
        }
        for (int i = 0; i < distinctFileIds.size(); i++) {
            FreightDemandAttachmentEntity entity = new FreightDemandAttachmentEntity();
            entity.setDemandId(demandId);
            entity.setFileAssetId(distinctFileIds.get(i));
            entity.setSortNo(i);
            entity.setDeleted(0);
            attachmentMapper.insert(entity);
        }
    }

    private FreightDemandVO toVO(FreightDemandEntity entity) {
        return new FreightDemandVO(
                entity.getId(),
                entity.getDemandNo(),
                entity.getTitle(),
                entity.getGoodsName(),
                entity.getDeparturePort(),
                entity.getDestinationPort(),
                entity.getDemandStatus(),
                entity.getAuditStatus(),
                entity.getBudgetAmount(),
                entity.getCurrencyCode(),
                entity.getCreatedAt()
        );
    }

    private MarketDemandAdminVO toAdminVO(FreightDemandEntity entity) {
        return new MarketDemandAdminVO(
                entity.getId(),
                entity.getDemandNo(),
                entity.getPublisherUserId(),
                entity.getTitle(),
                entity.getGoodsName(),
                entity.getDeparturePort(),
                entity.getDestinationPort(),
                entity.getDemandStatus(),
                entity.getAuditStatus(),
                entity.getCreatedAt()
        );
    }

    private FreightQuoteVO toQuoteVO(FreightQuoteEntity entity) {
        return new FreightQuoteVO(
                entity.getId(),
                entity.getDemandId(),
                entity.getQuoteNo(),
                entity.getPriceAmount(),
                entity.getCurrencyCode(),
                entity.getEstimatedDays(),
                entity.getServiceNote(),
                entity.getQuoteStatus(),
                entity.getCreatedAt()
        );
    }

    private FreightOrderVO toOrderVO(FreightOrderEntity entity) {
        return new FreightOrderVO(
                entity.getId(),
                entity.getOrderNo(),
                entity.getDemandId(),
                entity.getAcceptedQuoteId(),
                entity.getOrderStatus(),
                entity.getCreatedAt()
        );
    }

    private String buildDemandNo() {
        return "FD-" + LocalDate.now().toString().replace("-", "") + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private String buildQuoteNo() {
        return "FQ-" + LocalDate.now().toString().replace("-", "") + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private String buildOrderNo() {
        return "FO-" + LocalDate.now().toString().replace("-", "") + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private long normalizePageNo(Long value) {
        return value == null || value < 1 ? 1L : value;
    }

    private long normalizePageSize(Long value) {
        return value == null || value < 1 ? 10L : value;
    }

    private FreightDemandEntity requireDemand(Long demandId) {
        FreightDemandEntity entity = demandMapper.selectById(demandId);
        if (entity == null || entity.getDeleted() != null && entity.getDeleted() == 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "货运需求不存在");
        }
        return entity;
    }

    private FreightQuoteEntity requireQuote(Long quoteId, Long demandId) {
        FreightQuoteEntity entity = requireExistingQuote(quoteId, demandId);
        if (!"SUBMITTED".equals(entity.getQuoteStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "当前报价不可接单");
        }
        return entity;
    }

    private FreightQuoteEntity requireExistingQuote(Long quoteId, Long demandId) {
        FreightQuoteEntity entity = quoteMapper.selectById(quoteId);
        if (entity == null
                || entity.getDeleted() != null && entity.getDeleted() == 1
                || !demandId.equals(entity.getDemandId())) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "报价不存在");
        }
        return entity;
    }

    private FreightOrderEntity requireOrder(Long orderId) {
        FreightOrderEntity entity = orderMapper.selectById(orderId);
        if (entity == null || entity.getDeleted() != null && entity.getDeleted() == 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "货运订单不存在");
        }
        return entity;
    }

    private void assertPublisher(FreightDemandEntity demand) {
        if (!userRequestContext.currentUserId().equals(demand.getPublisherUserId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN.getCode(), "仅发布方可操作该需求");
        }
    }

    private void assertQuoter(FreightQuoteEntity quote) {
        if (!userRequestContext.currentUserId().equals(quote.getQuoterUserId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN.getCode(), "仅报价方可操作该报价");
        }
    }

    private FreightDemandAcceptedMessage buildAcceptedMessage(Long demandId, Long quoteId, Long orderId) {
        FreightDemandAcceptedMessage message = new FreightDemandAcceptedMessage();
        message.setDemandId(demandId);
        message.setQuoteId(quoteId);
        message.setOrderId(orderId);
        message.setOperatorUserId(userRequestContext.currentUserId());
        message.setTraceId(userRequestContext.currentTraceId());
        return message;
    }

    private void rejectOtherQuotes(Long demandId, Long acceptedQuoteId) {
        List<FreightQuoteEntity> others = quoteMapper.selectList(new LambdaQueryWrapper<FreightQuoteEntity>()
                .eq(FreightQuoteEntity::getDemandId, demandId)
                .eq(FreightQuoteEntity::getDeleted, 0));
        for (FreightQuoteEntity other : others) {
            if (!acceptedQuoteId.equals(other.getId()) && !"REJECTED".equals(other.getQuoteStatus())) {
                other.setQuoteStatus("REJECTED");
                other.setUpdatedBy(userRequestContext.currentUserId());
                other.setUpdatedAt(LocalDateTime.now());
                quoteMapper.updateById(other);
            }
        }
    }

    private void rejectOpenQuotes(Long demandId) {
        List<FreightQuoteEntity> quotes = quoteMapper.selectList(new LambdaQueryWrapper<FreightQuoteEntity>()
                .eq(FreightQuoteEntity::getDemandId, demandId)
                .eq(FreightQuoteEntity::getDeleted, 0));
        for (FreightQuoteEntity quote : quotes) {
            if ("SUBMITTED".equals(quote.getQuoteStatus())) {
                quote.setQuoteStatus("REJECTED");
                quote.setUpdatedBy(userRequestContext.currentUserId());
                quoteMapper.updateById(quote);
            }
        }
    }

    private boolean hasOpenQuotes(Long demandId, Long excludingQuoteId) {
        List<FreightQuoteEntity> quotes = quoteMapper.selectList(new LambdaQueryWrapper<FreightQuoteEntity>()
                .eq(FreightQuoteEntity::getDemandId, demandId)
                .eq(FreightQuoteEntity::getDeleted, 0));
        return quotes.stream()
                .anyMatch(quote -> !excludingQuoteId.equals(quote.getId()) && "SUBMITTED".equals(quote.getQuoteStatus()));
    }

    private void insertTimeline(Long orderId, String eventType, String eventMessage) {
        FreightOrderTimelineEntity entity = new FreightOrderTimelineEntity();
        entity.setOrderId(orderId);
        entity.setEventType(eventType);
        entity.setEventMessage(eventMessage);
        entity.setOperatorUserId(userRequestContext.currentUserId());
        timelineMapper.insert(entity);
    }
}
