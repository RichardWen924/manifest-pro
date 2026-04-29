package com.manifestreader.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.manifestreader.common.exception.BusinessException;
import com.manifestreader.common.exception.ErrorCode;
import com.manifestreader.common.result.PageResult;
import com.manifestreader.model.entity.BlChargeEntity;
import com.manifestreader.model.entity.BlCargoItemEntity;
import com.manifestreader.model.entity.BlDocumentEntity;
import com.manifestreader.model.entity.BlIssueInfoEntity;
import com.manifestreader.model.entity.BlPartyEntity;
import com.manifestreader.user.dify.DifyTemplateExportParser;
import com.manifestreader.user.dify.DifyWorkflowClient;
import com.manifestreader.user.mapper.UserBillMapper;
import com.manifestreader.user.mapper.UserCargoItemMapper;
import com.manifestreader.user.mapper.UserChargeMapper;
import com.manifestreader.user.mapper.UserIssueInfoMapper;
import com.manifestreader.user.mapper.UserPartyMapper;
import com.manifestreader.user.model.dto.BillCreateRequest;
import com.manifestreader.user.model.dto.BillExtractSaveRequest;
import com.manifestreader.user.model.dto.BillPageQuery;
import com.manifestreader.user.model.dto.BillParseRequest;
import com.manifestreader.user.model.dto.BillUpdateRequest;
import com.manifestreader.user.model.dto.ExtractedBillSaveRequest;
import com.manifestreader.user.model.vo.BillExtractFieldVO;
import com.manifestreader.user.model.vo.BillExtractResultVO;
import com.manifestreader.user.model.vo.BillExtractTaskSubmitVO;
import com.manifestreader.user.model.vo.BillExtractTaskVO;
import com.manifestreader.user.model.vo.BillDetailVO;
import com.manifestreader.user.model.vo.BillVO;
import com.manifestreader.user.service.BillParseTaskService;
import com.manifestreader.user.service.BillService;
import java.io.IOException;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.Collections;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class BillServiceImpl implements BillService {

    private static final Long DEFAULT_COMPANY_ID = 2L;
    private static final Long DEFAULT_USER_ID = 3L;

    private final UserBillMapper billMapper;
    private final UserCargoItemMapper cargoItemMapper;
    private final UserPartyMapper partyMapper;
    private final UserChargeMapper chargeMapper;
    private final UserIssueInfoMapper issueInfoMapper;
    private final DifyWorkflowClient difyWorkflowClient;
    private final DifyTemplateExportParser exportParser;
    private final BillParseTaskService billParseTaskService;
    private final ConcurrentMap<String, BillExtractResultVO> billExtractCache = new ConcurrentHashMap<>();

    public BillServiceImpl(
            UserBillMapper billMapper,
            UserCargoItemMapper cargoItemMapper,
            UserPartyMapper partyMapper,
            UserChargeMapper chargeMapper,
            UserIssueInfoMapper issueInfoMapper,
            DifyWorkflowClient difyWorkflowClient,
            DifyTemplateExportParser exportParser,
            BillParseTaskService billParseTaskService
    ) {
        this.billMapper = billMapper;
        this.cargoItemMapper = cargoItemMapper;
        this.partyMapper = partyMapper;
        this.chargeMapper = chargeMapper;
        this.issueInfoMapper = issueInfoMapper;
        this.difyWorkflowClient = difyWorkflowClient;
        this.exportParser = exportParser;
        this.billParseTaskService = billParseTaskService;
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
    public BillVO saveExtractedFields(ExtractedBillSaveRequest request) {
        Map<String, Object> fields = request.fields();
        String blNo = firstText(fields, "bl_no", "booking_no");
        if (!StringUtils.hasText(blNo)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "抽取结果缺少 bl_no 或 booking_no，无法保存为业务提单");
        }

        BlDocumentEntity entity = billMapper.selectOne(new LambdaQueryWrapper<BlDocumentEntity>()
                .eq(BlDocumentEntity::getCompanyId, DEFAULT_COMPANY_ID)
                .eq(BlDocumentEntity::getBlNo, blNo)
                .eq(BlDocumentEntity::getDeleted, 0)
                .last("LIMIT 1"));
        boolean creating = entity == null;
        if (creating) {
            entity = new BlDocumentEntity();
            entity.setCompanyId(DEFAULT_COMPANY_ID);
            entity.setCreatedBy(DEFAULT_USER_ID);
            entity.setCreatedAt(LocalDateTime.now());
            entity.setDeleted(0);
        }

        entity.setBlNo(blNo);
        entity.setBookingNo(text(fields, "booking_no"));
        entity.setDocNo(text(fields, "doc_no"));
        entity.setSerialNo(text(fields, "serial_no"));
        entity.setVesselVoyage(text(fields, "vessel_voyage"));
        entity.setPreCarriageBy(text(fields, "pre_carriage_by"));
        entity.setPlaceOfReceipt(text(fields, "place_of_receipt"));
        entity.setPortOfLoading(text(fields, "port_of_loading"));
        entity.setPortOfDischarge(text(fields, "port_of_discharge"));
        entity.setPlaceOfDelivery(text(fields, "place_of_delivery"));
        entity.setStatus("CONFIRMED");
        entity.setParseStatus("SUCCESS");
        entity.setRemark(buildExtractedRemark(request));
        entity.setUpdatedBy(DEFAULT_USER_ID);
        entity.setUpdatedAt(LocalDateTime.now());

        if (creating) {
            billMapper.insert(entity);
        } else {
            billMapper.updateById(entity);
        }
        replaceExtractedChildren(entity.getId(), fields);
        return toVO(entity, findFirstCargo(entity.getId()));
    }

    @Override
    public BillExtractResultVO extractBill(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "请上传需要提取的提单文件");
        }
        String extractId = hashFile(file);
        BillExtractResultVO cached = billExtractCache.get(extractId);
        if (cached != null) {
            return cached;
        }
        synchronized (billExtractCache) {
            cached = billExtractCache.get(extractId);
            if (cached != null) {
                return cached;
            }
            String difyResponse = difyWorkflowClient.runBillExtraction(file);
            DifyTemplateExportParser.ParsedExportFields parsed = exportParser.parse(difyResponse);
            BillExtractResultVO result = toBillExtractResult(extractId, file.getOriginalFilename(), parsed);
            billExtractCache.put(extractId, result);
            return result;
        }
    }

    @Override
    public BillExtractTaskSubmitVO submitExtractTask(MultipartFile file) {
        return billParseTaskService.submitExtractTask(file);
    }

    @Override
    public BillExtractTaskVO getExtractTask(String taskNo) {
        return billParseTaskService.getTask(taskNo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BillVO saveExtractedResult(BillExtractSaveRequest request) {
        Map<String, Object> fields = request.fields();
        if (fields == null || fields.isEmpty()) {
            BillExtractResultVO cached = billExtractCache.get(request.extractId());
            if (cached != null && cached.fields() != null && !cached.fields().isEmpty()) {
                fields = cached.fields();
            } else {
                BillExtractResultVO asyncResult = billParseTaskService.resolveResult(request.extractId());
                if (asyncResult == null || asyncResult.fields() == null || asyncResult.fields().isEmpty()) {
                    throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "提单提取结果不存在或已过期，请重新上传提取");
                }
                fields = asyncResult.fields();
            }
        }
        return saveExtractedFields(new ExtractedBillSaveRequest(
                request.templateId(),
                request.sourceFileName(),
                fields
        ));
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

    private void replaceExtractedChildren(Long billId, Map<String, Object> fields) {
        cargoItemMapper.delete(new LambdaQueryWrapper<BlCargoItemEntity>().eq(BlCargoItemEntity::getBlDocumentId, billId));
        partyMapper.delete(new LambdaQueryWrapper<BlPartyEntity>().eq(BlPartyEntity::getBlDocumentId, billId));
        chargeMapper.delete(new LambdaQueryWrapper<BlChargeEntity>().eq(BlChargeEntity::getBlDocumentId, billId));
        issueInfoMapper.delete(new LambdaQueryWrapper<BlIssueInfoEntity>().eq(BlIssueInfoEntity::getBlDocumentId, billId));

        saveExtractedCargo(billId, fields);
        saveParty(billId, "SHIPPER", text(fields, "shipper"), 1);
        saveParty(billId, "CONSIGNEE", text(fields, "consignee"), 2);
        saveParty(billId, "NOTIFY_PARTY", text(fields, "notify_party"), 3);
        saveParty(billId, "DELIVERY_AGENT", text(fields, "delivery_agent"), 4);
        saveParty(billId, "CARRIER_AGENT", text(fields, "carrier_agent"), 5);
        saveExtractedCharge(billId, fields);
        saveExtractedIssueInfo(billId, fields);
    }

    private void saveExtractedCargo(Long billId, Map<String, Object> fields) {
        if (!hasAny(fields, "goods_description", "package_quantity", "container_no", "seal_no", "gross_weight_kgs", "measurement_cbm")) {
            return;
        }
        BlCargoItemEntity cargo = new BlCargoItemEntity();
        cargo.setBlDocumentId(billId);
        cargo.setContainerNo(text(fields, "container_no"));
        cargo.setSealNo(text(fields, "seal_no"));
        cargo.setContainerType(text(fields, "revenue_tons"));
        cargo.setPackageQuantity(integer(fields, "package_quantity"));
        cargo.setPackageUnit(textOrDefault(fields, "package_unit", "CTN"));
        cargo.setGoodsDescription(text(fields, "goods_description"));
        cargo.setMarks(text(fields, "marks"));
        cargo.setContainerWeight(decimal(fields, "container_weight"));
        cargo.setVgmWeight(decimal(fields, "vgm_weight"));
        cargo.setGrossWeightKgs(decimal(fields, "gross_weight_kgs"));
        cargo.setMeasurementCbm(decimal(fields, "measurement_cbm"));
        cargo.setSortNo(1);
        cargoItemMapper.insert(cargo);
    }

    private void saveParty(Long billId, String partyType, String rawText, int sortNo) {
        if (!StringUtils.hasText(rawText)) {
            return;
        }
        BlPartyEntity party = new BlPartyEntity();
        party.setBlDocumentId(billId);
        party.setPartyType(partyType);
        party.setPartyName(firstLine(rawText));
        party.setPartyAddress(rawText);
        party.setSortNo(sortNo);
        partyMapper.insert(party);
    }

    private void saveExtractedCharge(Long billId, Map<String, Object> fields) {
        if (!hasAny(fields, "freight_term", "prepaid_amount", "collect_amount", "payable_at")) {
            return;
        }
        BlChargeEntity charge = new BlChargeEntity();
        charge.setBlDocumentId(billId);
        charge.setChargeType("FREIGHT");
        charge.setChargeName(textOrDefault(fields, "freight_term", "FREIGHT"));
        charge.setPaymentTerm(firstText(fields, "freight_term", "service_mode"));
        charge.setPayableAt(text(fields, "payable_at"));
        charge.setAmount(firstDecimal(fields, "prepaid_amount", "collect_amount"));
        charge.setSortNo(1);
        chargeMapper.insert(charge);
    }

    private void saveExtractedIssueInfo(Long billId, Map<String, Object> fields) {
        if (!hasAny(fields, "service_type", "service_mode", "revenue_tons", "issue_place", "laden_on_board", "original_bl_count")) {
            return;
        }
        BlIssueInfoEntity issueInfo = new BlIssueInfoEntity();
        issueInfo.setBlDocumentId(billId);
        issueInfo.setServiceType(text(fields, "service_type"));
        issueInfo.setServiceMode(text(fields, "service_mode"));
        issueInfo.setRevenueTons(text(fields, "revenue_tons"));
        issueInfo.setIssuePlace(text(fields, "issue_place"));
        issueInfo.setLadenOnBoard(text(fields, "laden_on_board"));
        issueInfo.setOriginalBlCount(text(fields, "original_bl_count"));
        issueInfoMapper.insert(issueInfo);
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

    private String buildExtractedRemark(ExtractedBillSaveRequest request) {
        String source = StringUtils.hasText(request.sourceFileName()) ? request.sourceFileName() : "Dify 抽取结果";
        return "由模板导出抽取结果保存：" + source;
    }

    private BillExtractResultVO toBillExtractResult(
            String extractId,
            String fileName,
            DifyTemplateExportParser.ParsedExportFields parsed
    ) {
        Map<String, Object> fields = parsed.fields();
        List<BillExtractFieldVO> fieldList = fields.entrySet().stream()
                .map(entry -> new BillExtractFieldVO(entry.getKey(), entry.getValue()))
                .toList();
        String blNo = firstText(fields, "bl_no", "booking_no");
        String status = StringUtils.hasText(blNo) ? "READY_TO_SAVE" : "REVIEW_REQUIRED";
        String message = StringUtils.hasText(blNo)
                ? "提单字段已提取，请确认后保存为业务数据"
                : "提取结果缺少 bl_no/booking_no，请人工补齐后再保存";
        return new BillExtractResultVO(
                extractId,
                StringUtils.hasText(fileName) ? fileName : "bill-file",
                fields.size(),
                fields,
                fieldList,
                parsed.rawText(),
                status,
                message
        );
    }

    private String hashFile(MultipartFile file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(file.getBytes()));
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR.getCode(), "读取上传文件失败");
        } catch (NoSuchAlgorithmException ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR.getCode(), "文件指纹算法不可用");
        }
    }

    private boolean hasAny(Map<String, Object> fields, String... keys) {
        for (String key : keys) {
            if (StringUtils.hasText(text(fields, key))) {
                return true;
            }
        }
        return false;
    }

    private String firstText(Map<String, Object> fields, String... keys) {
        for (String key : keys) {
            String value = text(fields, key);
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return "";
    }

    private String textOrDefault(Map<String, Object> fields, String key, String fallback) {
        String value = text(fields, key);
        return StringUtils.hasText(value) ? value : fallback;
    }

    private String text(Map<String, Object> fields, String key) {
        if (fields == null || !fields.containsKey(key) || fields.get(key) == null) {
            return "";
        }
        return String.valueOf(fields.get(key)).trim();
    }

    private String firstLine(String text) {
        String value = text == null ? "" : text.strip();
        int newline = value.indexOf('\n');
        int carriageReturn = value.indexOf('\r');
        int index = newline < 0 ? carriageReturn : (carriageReturn < 0 ? newline : Math.min(newline, carriageReturn));
        return index < 0 ? value : value.substring(0, index).trim();
    }

    private Integer integer(Map<String, Object> fields, String key) {
        String value = text(fields, key).replaceAll("[^0-9-]", "");
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private BigDecimal firstDecimal(Map<String, Object> fields, String... keys) {
        for (String key : keys) {
            BigDecimal value = decimal(fields, key);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private BigDecimal decimal(Map<String, Object> fields, String key) {
        String value = text(fields, key).replaceAll("[^0-9.\\-]", "");
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
