package com.manifestreader.model.vo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record FreightDemandDetailVO(
        Long id,
        String demandNo,
        String title,
        String goodsName,
        String departurePort,
        String destinationPort,
        LocalDate expectedShippingDate,
        BigDecimal quantity,
        String quantityUnit,
        BigDecimal budgetAmount,
        String currencyCode,
        String contactName,
        String contactPhone,
        String remark,
        String demandStatus,
        String auditStatus,
        Long acceptedQuoteId,
        Long acceptedOrderId,
        List<Long> attachmentFileIds,
        LocalDateTime createdAt
) {
}
