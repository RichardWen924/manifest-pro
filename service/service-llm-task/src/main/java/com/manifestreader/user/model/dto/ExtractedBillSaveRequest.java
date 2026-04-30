package com.manifestreader.user.model.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.Map;

public record ExtractedBillSaveRequest(
        Long templateId,
        String sourceFileName,
        @NotEmpty Map<String, Object> fields
) {
}
