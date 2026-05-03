package com.manifestreader.market.support;

public interface UserRequestContext {

    Long currentCompanyId();

    Long currentUserId();

    String currentTraceId();
}
