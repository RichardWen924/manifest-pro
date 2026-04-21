package com.manifestreader.common.exception;

public class BizException extends RuntimeException {

    private final String code;

    public BizException(String message) {
        this("BIZ_ERROR", message);
    }

    public BizException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
