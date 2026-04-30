package com.manifestreader.user.model.dto;

import jakarta.validation.constraints.NotBlank;

public record BillCreateRequest(
        @NotBlank String blNo,
        String bookingNo,
        String vesselVoyage,
        String portOfLoading,
        String portOfDischarge,
        String placeOfReceipt,
        String placeOfDelivery,
        String goodsName,
        Integer quantity,
        String packageUnit,
        Long sourceFileId,
        String status,
        String remark
) {
}
