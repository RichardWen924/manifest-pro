package com.manifestreader.common.util;

import java.util.UUID;

public final class TraceIdUtil {

    private static final ThreadLocal<String> TRACE_ID_HOLDER = new ThreadLocal<>();

    private TraceIdUtil() {
    }

    public static String getOrCreateTraceId() {
        String traceId = TRACE_ID_HOLDER.get();
        if (traceId == null || traceId.isBlank()) {
            traceId = createTraceId();
            TRACE_ID_HOLDER.set(traceId);
        }
        return traceId;
    }

    public static void setTraceId(String traceId) {
        TRACE_ID_HOLDER.set(traceId);
    }

    public static void clear() {
        TRACE_ID_HOLDER.remove();
    }

    public static String createTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
