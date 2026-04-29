package com.manifestreader.user.support;

public interface UserRequestContext {

    Long currentCompanyId();

    Long currentUserId();

    String currentTraceId();
}
