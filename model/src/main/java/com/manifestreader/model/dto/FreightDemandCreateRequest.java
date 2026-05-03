package com.manifestreader.model.dto;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record FreightDemandCreateRequest(
        @NotBlank String title,
        @NotBlank String goodsName,
        @NotBlank String departurePort,
        @NotBlank String destinationPort,
        LocalDate expectedShippingDate,
        BigDecimal quantity,
        String quantityUnit,
        BigDecimal budgetAmount,
        String currencyCode,
        String contactName,
        String contactPhone,
        String remark,
        List<Long> attachmentFileIds
) {
}
