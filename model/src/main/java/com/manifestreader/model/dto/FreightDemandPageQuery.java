package com.manifestreader.model.dto;

public record FreightDemandPageQuery(
        String keyword,
        String status,
        Long pageNo,
        Long pageSize
) {
    public Long pageNo() {
        return pageNo == null || pageNo < 1 ? 1L : pageNo;
    }

    public Long pageSize() {
        return pageSize == null || pageSize < 1 ? 10L : pageSize;
    }
}
