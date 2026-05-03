package com.manifestreader.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record FreightQuoteCreateRequest(
        @NotNull BigDecimal priceAmount,
        @NotBlank String currencyCode,
        Integer estimatedDays,
        String serviceNote
) {
}
