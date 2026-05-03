package com.manifestreader.model.vo;

import java.time.LocalDateTime;

public record FreightOrderVO(
        Long id,
        String orderNo,
        Long demandId,
        Long acceptedQuoteId,
        String orderStatus,
        LocalDateTime createdAt
) {
}
