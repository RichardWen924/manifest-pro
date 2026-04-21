package com.manifestreader.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.manifestreader.common.exception.BusinessException;
import com.manifestreader.common.exception.ErrorCode;
import com.manifestreader.common.result.PageResult;
import com.manifestreader.model.entity.BlCargoItemEntity;
import com.manifestreader.model.entity.BlDocumentEntity;
import com.manifestreader.user.mapper.UserBillMapper;
import com.manifestreader.user.mapper.UserCargoItemMapper;
import com.manifestreader.user.model.dto.BillCreateRequest;
import com.manifestreader.user.model.dto.BillPageQuery;
import com.manifestreader.user.model.dto.BillParseRequest;
import com.manifestreader.user.model.dto.BillUpdateRequest;
import com.manifestreader.user.model.vo.BillDetailVO;
import com.manifestreader.user.model.vo.BillVO;
import com.manifestreader.user.service.BillService;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class BillServiceImpl implements BillService {

    private static final Long DEFAULT_COMPANY_ID = 2L;
    private static final Long DEFAULT_USER_ID = 3L;

    private final UserBillMapper billMapper;
    private final UserCargoItemMapper cargoItemMapper;

    public BillServiceImpl(UserBillMapper billMapper, UserCargoItemMapper cargoItemMapper) {
        this.billMapper = billMapper;
        this.cargoItemMapper = cargoItemMapper;
    }

    @Override
    public PageResult<BillVO> page(BillPageQuery query) {
        Page<BlDocumentEntity> page = billMapper.selectPage(
                Page.of(query.pageNo(), query.pageSize()),
                buildPageWrapper(query)
        );
        List<Long> billIds = page.getRecords().stream().map(BlDocumentEntity::getId).toList();
        Map<Long, BlCargoItemEntity> cargoMap = findFirstCargoMap(billIds);

        PageResult<BillVO> result = PageResult.empty(page.getCurrent(), page.getSize());
        result.setTotal(page.getTotal());
        result.setRecords(page.getRecords().stream()
                .map(entity -> toVO(entity, cargoMap.get(entity.getId())))
                .toList());
        return result;
    }

    @Override
    public BillDetailVO detail(Long id) {
        BlDocumentEntity entity = findActiveBill(id);
        BlCargoItemEntity cargo = findFirstCargo(id);
        return new BillDetailVO(
                entity.getId(),
                entity.getBlNo(),
                entity.getBookingNo(),
                entity.getVesselVoyage(),
                entity.getPortOfLoading(),
                entity.getPortOfDischarge(),
                entity.getPlaceOfReceipt(),
                entity.getPlaceOfDelivery(),
                cargo == null ? null : cargo.getGoodsDescription(),
                formatQuantity(cargo),
                entity.getStatus(),
                entity.getParseStatus(),
                entity.getRemark(),
                Collections.emptyList(),
                cargo == null ? Collections.emptyList() : List.of(cargo.getGoodsDescription()),
                Collections.emptyList()
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BillVO create(BillCreateRequest request) {
        BlDocumentEntity entity = new BlDocumentEntity();
        entity.setCompanyId(DEFAULT_COMPANY_ID);
        entity.setBlNo(request.blNo());
        entity.setBookingNo(request.bookingNo());
        entity.setVesselVoyage(request.vesselVoyage());
        entity.setPortOfLoading(request.portOfLoading());
        entity.setPortOfDischarge(request.portOfDischarge());
        entity.setPlaceOfReceipt(request.placeOfReceipt());
        entity.setPlaceOfDelivery(request.placeOfDelivery());
        entity.setSourceFileId(request.sourceFileId());
        entity.setStatus(StringUtils.hasText(request.status()) ? request.status() : "DRAFT");
        entity.setParseStatus("NONE");
        entity.setRemark(request.remark());
        entity.setCreatedBy(DEFAULT_USER_ID);
        entity.setUpdatedBy(DEFAULT_USER_ID);
        entity.setDeleted(0);

        try {
            billMapper.insert(entity);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "提单号已存在");
        }
        upsertFirstCargo(entity.getId(), request.goodsName(), request.quantity(), request.packageUnit());
        return toVO(entity, findFirstCargo(entity.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BillVO update(Long id, BillUpdateRequest request) {
        BlDocumentEntity entity = findActiveBill(id);
        if (StringUtils.hasText(request.blNo())) {
            entity.setBlNo(request.blNo());
        }
        entity.setBookingNo(request.bookingNo());
        entity.setVesselVoyage(request.vesselVoyage());
        entity.setPortOfLoading(request.portOfLoading());
        entity.setPortOfDischarge(request.portOfDischarge());
        entity.setPlaceOfReceipt(request.placeOfReceipt());
        entity.setPlaceOfDelivery(request.placeOfDelivery());
        if (StringUtils.hasText(request.status())) {
            entity.setStatus(request.status());
        }
        entity.setRemark(request.remark());
        entity.setUpdatedBy(DEFAULT_USER_ID);
        entity.setUpdatedAt(LocalDateTime.now());

        try {
            billMapper.updateById(entity);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "提单号已存在");
        }
        upsertFirstCargo(entity.getId(), request.goodsName(), request.quantity(), request.packageUnit());
        return toVO(entity, findFirstCargo(entity.getId()));
    }

    @Override
    public void delete(Long id) {
        BlDocumentEntity entity = findActiveBill(id);
        entity.setDeleted(1);
        entity.setUpdatedBy(DEFAULT_USER_ID);
        entity.setUpdatedAt(LocalDateTime.now());
        billMapper.updateById(entity);
    }

    @Override
    public BillDetailVO parse(BillParseRequest request) {
        // TODO 后续接入提单解析流程，本阶段不实现复杂解析逻辑。
        throw new BusinessException(ErrorCode.NOT_IMPLEMENTED);
    }

    private LambdaQueryWrapper<BlDocumentEntity> buildPageWrapper(BillPageQuery query) {
        LambdaQueryWrapper<BlDocumentEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BlDocumentEntity::getCompanyId, DEFAULT_COMPANY_ID)
                .eq(BlDocumentEntity::getDeleted, 0);
        if (StringUtils.hasText(query.keyword())) {
            wrapper.and(item -> item
                    .like(BlDocumentEntity::getBlNo, query.keyword())
                    .or()
                    .like(BlDocumentEntity::getBookingNo, query.keyword())
                    .or()
                    .like(BlDocumentEntity::getVesselVoyage, query.keyword()));
        }
        if (StringUtils.hasText(query.status())) {
            wrapper.eq(BlDocumentEntity::getStatus, query.status());
        }
        wrapper.orderByDesc(BlDocumentEntity::getCreatedAt);
        return wrapper;
    }

    private BlDocumentEntity findActiveBill(Long id) {
        BlDocumentEntity entity = billMapper.selectOne(new LambdaQueryWrapper<BlDocumentEntity>()
                .eq(BlDocumentEntity::getId, id)
                .eq(BlDocumentEntity::getCompanyId, DEFAULT_COMPANY_ID)
                .eq(BlDocumentEntity::getDeleted, 0));
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return entity;
    }

    private Map<Long, BlCargoItemEntity> findFirstCargoMap(List<Long> billIds) {
        if (billIds == null || billIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return cargoItemMapper.selectList(new LambdaQueryWrapper<BlCargoItemEntity>()
                        .in(BlCargoItemEntity::getBlDocumentId, billIds)
                        .orderByAsc(BlCargoItemEntity::getSortNo)
                        .orderByAsc(BlCargoItemEntity::getId))
                .stream()
                .collect(Collectors.toMap(
                        BlCargoItemEntity::getBlDocumentId,
                        Function.identity(),
                        (first, ignored) -> first
                ));
    }

    private BlCargoItemEntity findFirstCargo(Long billId) {
        return cargoItemMapper.selectOne(new LambdaQueryWrapper<BlCargoItemEntity>()
                .eq(BlCargoItemEntity::getBlDocumentId, billId)
                .orderByAsc(BlCargoItemEntity::getSortNo)
                .orderByAsc(BlCargoItemEntity::getId)
                .last("LIMIT 1"));
    }

    private void upsertFirstCargo(Long billId, String goodsName, Integer quantity, String packageUnit) {
        if (!StringUtils.hasText(goodsName) && quantity == null) {
            return;
        }
        BlCargoItemEntity cargo = findFirstCargo(billId);
        if (cargo == null) {
            cargo = new BlCargoItemEntity();
            cargo.setBlDocumentId(billId);
            cargo.setSortNo(1);
        }
        cargo.setGoodsDescription(goodsName);
        cargo.setPackageQuantity(quantity);
        cargo.setPackageUnit(StringUtils.hasText(packageUnit) ? packageUnit : "CTN");
        if (cargo.getId() == null) {
            cargoItemMapper.insert(cargo);
        } else {
            cargoItemMapper.updateById(cargo);
        }
    }

    private BillVO toVO(BlDocumentEntity entity, BlCargoItemEntity cargo) {
        return new BillVO(
                entity.getId(),
                entity.getBlNo(),
                entity.getBookingNo(),
                entity.getVesselVoyage(),
                entity.getPortOfLoading(),
                entity.getPortOfDischarge(),
                cargo == null ? null : cargo.getGoodsDescription(),
                formatQuantity(cargo),
                entity.getStatus(),
                entity.getParseStatus(),
                entity.getCreatedAt()
        );
    }

    private String formatQuantity(BlCargoItemEntity cargo) {
        if (cargo == null || cargo.getPackageQuantity() == null) {
            return null;
        }
        return cargo.getPackageQuantity() + " " + (StringUtils.hasText(cargo.getPackageUnit()) ? cargo.getPackageUnit() : "CTN");
    }
}
