package com.agnostic.producerspringboot;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Locale;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
class RequestLoggingContextFilter extends OncePerRequestFilter {
    private static final String TRACE_ID = "trace_id";
    private static final String SPAN_ID = "span_id";
    private static final String HTTP_METHOD = "http_method";
    private static final String HTTP_PATH = "http_path";

    @Override
    protected void doFilterInternal(
        HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        String traceparent = trim(request.getHeader("traceparent"));
        ParsedTraceparent parsedTraceparent = ParsedTraceparent.parse(traceparent);

        String traceId = parsedTraceparent.traceId();
        if (traceId == null) {
            traceId = firstNonBlank(request.getHeader("x-b3-traceid"), request.getHeader("x-request-id"));
        }
        if (traceId == null) {
            traceId = "unknown";
        }

        String spanId = parsedTraceparent.spanId();
        if (spanId == null) {
            spanId = trim(request.getHeader("x-b3-spanid"));
        }
        if (spanId == null) {
            spanId = "unknown";
        }

        MDC.put(TRACE_ID, traceId.toLowerCase(Locale.ROOT));
        MDC.put(SPAN_ID, spanId.toLowerCase(Locale.ROOT));
        MDC.put(HTTP_METHOD, request.getMethod());
        MDC.put(HTTP_PATH, request.getRequestURI());
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(TRACE_ID);
            MDC.remove(SPAN_ID);
            MDC.remove(HTTP_METHOD);
            MDC.remove(HTTP_PATH);
        }
    }

    private static String firstNonBlank(String first, String second) {
        String firstValue = trim(first);
        if (firstValue != null) {
            return firstValue;
        }
        return trim(second);
    }

    private static String trim(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private record ParsedTraceparent(String traceId, String spanId) {
        static ParsedTraceparent parse(String traceparent) {
            if (traceparent == null) {
                return new ParsedTraceparent(null, null);
            }

            String[] parts = traceparent.split("-");
            if (parts.length != 4) {
                return new ParsedTraceparent(null, null);
            }

            String traceId = parts[1];
            String spanId = parts[2];
            if (!isValidHex(traceId, 32) || !isValidHex(spanId, 16)) {
                return new ParsedTraceparent(null, null);
            }
            if (traceId.equals("00000000000000000000000000000000")
                || spanId.equals("0000000000000000")) {
                return new ParsedTraceparent(null, null);
            }
            return new ParsedTraceparent(traceId, spanId);
        }

        private static boolean isValidHex(String value, int expectedLength) {
            if (value == null || value.length() != expectedLength) {
                return false;
            }
            for (int i = 0; i < value.length(); i++) {
                char c = value.charAt(i);
                boolean isDigit = c >= '0' && c <= '9';
                boolean isLowerHex = c >= 'a' && c <= 'f';
                boolean isUpperHex = c >= 'A' && c <= 'F';
                if (!isDigit && !isLowerHex && !isUpperHex) {
                    return false;
                }
            }
            return true;
        }
    }
}
