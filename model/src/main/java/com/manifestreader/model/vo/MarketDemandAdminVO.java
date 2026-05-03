package com.manifestreader.model.vo;

import java.time.LocalDateTime;

public record MarketDemandAdminVO(
        Long id,
        String demandNo,
        Long publisherUserId,
        String title,
        String goodsName,
        String departurePort,
        String destinationPort,
        String demandStatus,
        String auditStatus,
        LocalDateTime createdAt
) {
}
