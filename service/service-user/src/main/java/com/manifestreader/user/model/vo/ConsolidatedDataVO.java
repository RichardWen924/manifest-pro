package com.manifestreader.user.model.vo;

import java.util.List;

public record ConsolidatedDataVO(
        Long billId,
        BillDetailVO bill,
        List<FileAssetVO> files,
        List<TemplateOptionVO> templates
) {
}
