package com.manifestreader.user.model.vo;

import java.util.List;
import java.util.Map;

public record BillExtractResultVO(
        String extractId,
        String fileName,
        Integer fieldCount,
        Map<String, Object> fields,
        List<BillExtractFieldVO> fieldList,
        String rawText,
        String status,
        String message
) {
}
