package com.manifestreader.model.vo;

import java.time.LocalDateTime;

public record BillSummaryVO(
        Long id,
        String blNo,
        String bookingNo,
        String vesselVoyage,
        String portOfLoading,
        String portOfDischarge,
        String goodsName,
        String quantity,
        String status,
        String parseStatus,
        LocalDateTime createdAt
) {
}
