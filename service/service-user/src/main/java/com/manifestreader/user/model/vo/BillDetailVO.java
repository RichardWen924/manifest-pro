package com.manifestreader.user.model.vo;

import java.util.List;

public record BillDetailVO(
        Long id,
        String blNo,
        String bookingNo,
        String vesselVoyage,
        String portOfLoading,
        String portOfDischarge,
        String placeOfReceipt,
        String placeOfDelivery,
        String goodsName,
        String quantity,
        String status,
        String parseStatus,
        String remark,
        List<String> parties,
        List<String> cargoItems,
        List<String> charges
) {
}
