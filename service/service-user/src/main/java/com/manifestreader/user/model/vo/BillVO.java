package com.manifestreader.user.model.vo;

import java.time.LocalDateTime;

public record BillVO(
        Long id,
        String blNo,
        String bookingNo,
        String vesselVoyage,
        String status,
        String parseStatus,
        LocalDateTime createdAt
) {
}
