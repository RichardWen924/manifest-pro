package com.manifestreader.common.result;

import java.util.Collections;
import java.util.List;

public class PageResult<T> {

    private long current;
    private long size;
    private long total;
    private List<T> records;

    public static <T> PageResult<T> empty() {
        PageResult<T> result = new PageResult<>();
        result.setCurrent(1L);
        result.setSize(10L);
        result.setTotal(0L);
        result.setRecords(Collections.emptyList());
        return result;
    }

    public static <T> PageResult<T> empty(long current, long size) {
        PageResult<T> result = new PageResult<>();
        result.setCurrent(current);
        result.setSize(size);
        result.setTotal(0L);
        result.setRecords(Collections.emptyList());
        return result;
    }

    public long getCurrent() {
        return current;
    }

    public void setCurrent(long current) {
        this.current = current;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public List<T> getRecords() {
        return records;
    }

    public void setRecords(List<T> records) {
        this.records = records;
    }
}
