package com.manifestreader.user.model.dto;

public record ConsolidatedQuery(
        Long billId,
        String keyword,
        Long pageNo,
        Long pageSize
) {
    public Long pageNo() {
        return pageNo == null || pageNo < 1 ? 1 : pageNo;
    }

    public Long pageSize() {
        return pageSize == null || pageSize < 1 ? 10 : pageSize;
    }
}
