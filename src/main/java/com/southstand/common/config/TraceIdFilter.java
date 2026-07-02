package com.southstand.common.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class TraceIdFilter extends OncePerRequestFilter {

    private static final String TRACE_HEADER = "X-Trace-Id";
    private static final String REQUEST_HEADER = "X-Request-Id";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String traceId = request.getHeader(TRACE_HEADER);
        if (traceId == null || traceId.isBlank()) {
            traceId = request.getHeader(REQUEST_HEADER);
        }
        if (traceId == null || traceId.isBlank()) {
            traceId = TraceIdHolder.getOrCreate();
        } else {
            TraceIdHolder.set(traceId);
        }

        response.setHeader(TRACE_HEADER, TraceIdHolder.getOrCreate());
        try {
            filterChain.doFilter(request, response);
        } finally {
            TraceIdHolder.clear();
        }
    }
}
