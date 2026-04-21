package com.manifestreader.user.model.dto;

public record BillUpdateRequest(
        String bookingNo,
        String vesselVoyage,
        String status,
        String remark
) {
}
