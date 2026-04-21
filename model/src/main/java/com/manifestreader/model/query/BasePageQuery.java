package com.manifestreader.model.query;

public class BasePageQuery {

    private long current = 1L;
    private long size = 10L;

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
}
