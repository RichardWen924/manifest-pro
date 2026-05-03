package com.manifestreader.model.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record FreightDemandVO(
        Long id,
        String demandNo,
        String title,
        String goodsName,
        String departurePort,
        String destinationPort,
        String demandStatus,
        String auditStatus,
        BigDecimal budgetAmount,
        String currencyCode,
        LocalDateTime createdAt
) {
}
