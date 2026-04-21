package com.manifestreader.user.model.dto;

import jakarta.validation.constraints.NotBlank;

public record BillCreateRequest(
        @NotBlank String blNo,
        String bookingNo,
        String vesselVoyage,
        Long sourceFileId,
        String remark
) {
}
