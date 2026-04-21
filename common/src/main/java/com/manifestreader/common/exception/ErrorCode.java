package com.manifestreader.common.exception;

public enum ErrorCode {

    SUCCESS("200", "success"),
    BAD_REQUEST("400", "请求参数错误"),
    UNAUTHORIZED("401", "未认证或登录已过期"),
    FORBIDDEN("403", "无访问权限"),
    NOT_FOUND("404", "资源不存在"),
    BUSINESS_ERROR("BIZ_ERROR", "业务处理失败"),
    NOT_IMPLEMENTED("NOT_IMPLEMENTED", "功能待实现"),
    INTERNAL_ERROR("500", "系统异常");

    private final String code;
    private final String message;

    ErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
