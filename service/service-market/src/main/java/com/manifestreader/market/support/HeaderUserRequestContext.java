package com.manifestreader.market.support;

import com.manifestreader.common.constant.HeaderConstants;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class HeaderUserRequestContext implements UserRequestContext {

    private static final Long DEFAULT_COMPANY_ID = 2L;
    private static final Long DEFAULT_USER_ID = 3L;

    @Override
    public Long currentCompanyId() {
        return readLongHeader(HeaderConstants.COMPANY_ID, DEFAULT_COMPANY_ID);
    }

    @Override
    public Long currentUserId() {
        return readLongHeader(HeaderConstants.USER_ID, DEFAULT_USER_ID);
    }

    @Override
    public String currentTraceId() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return UUID.randomUUID().toString();
        }
        String value = attributes.getRequest().getHeader(HeaderConstants.TRACE_ID);
        return StringUtils.hasText(value) ? value : UUID.randomUUID().toString();
    }

    private Long readLongHeader(String headerName, Long fallback) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return fallback;
        }
        String value = attributes.getRequest().getHeader(headerName);
        if (!StringUtils.hasText(value)) {
            return fallback;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }
}
