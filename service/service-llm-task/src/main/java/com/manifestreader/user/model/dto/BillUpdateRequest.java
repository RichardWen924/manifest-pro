package com.manifestreader.user.model.dto;

public record BillUpdateRequest(
        String blNo,
        String bookingNo,
        String vesselVoyage,
        String portOfLoading,
        String portOfDischarge,
        String placeOfReceipt,
        String placeOfDelivery,
        String goodsName,
        Integer quantity,
        String packageUnit,
        String status,
        String remark
) {
}
