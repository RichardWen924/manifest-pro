package com.manifestreader.model.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record FreightQuoteVO(
        Long id,
        Long demandId,
        String quoteNo,
        BigDecimal priceAmount,
        String currencyCode,
        Integer estimatedDays,
        String serviceNote,
        String quoteStatus,
        LocalDateTime createdAt
) {
}
