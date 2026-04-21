package com.manifestreader.user.model.vo;

import java.util.List;

public record BillDetailVO(
        Long id,
        String blNo,
        String bookingNo,
        String vesselVoyage,
        List<String> parties,
        List<String> cargoItems,
        List<String> charges
) {
}
