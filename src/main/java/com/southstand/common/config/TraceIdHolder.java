package com.southstand.common.config;

import java.util.UUID;

public final class TraceIdHolder {

    private static final ThreadLocal<String> TRACE_ID = new ThreadLocal<>();

    private TraceIdHolder() {
    }

    public static String getOrCreate() {
        String traceId = TRACE_ID.get();
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString().replace("-", "");
            TRACE_ID.set(traceId);
        }
        return traceId;
    }

    public static void set(String traceId) {
        TRACE_ID.set(traceId);
    }

    public static void clear() {
        TRACE_ID.remove();
    }
}
