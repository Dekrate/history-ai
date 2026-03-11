package com.historyai.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Filter that adds a unique trace ID to each HTTP request for distributed tracing.
 *
 * <p>Provides request tracking across the application by:</p>
 * <ul>
 *   <li>Generating or extracting a trace ID from the request</li>
 *   <li>Adding the trace ID to MDC for log correlation</li>
 *   <li>Including the trace ID in response headers</li>
 *   <li>Validating incoming trace IDs for security</li>
 * </ul>
 *
 * <p>The trace ID enables correlating logs across different services and
 * helps with debugging distributed systems.</p>
 *
 * @author HistoryAI Team
 * @version 1.0
 * @see org.slf4j.MDC
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(TraceIdFilter.class);
    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final String TRACE_ID_MDC_KEY = "traceId";
    private static final int MAX_TRACE_ID_LENGTH = 64;
    private static final Pattern UUID_PATTERN = Pattern.compile("^[a-fA-F0-9-]{36}$");

    /**
     * Filters each request to add or extract trace ID.
     *
     * @param request     the HTTP request
     * @param response    the HTTP response
     * @param filterChain the filter chain
     * @throws ServletException if servlet processing fails
     * @throws IOException      if I/O error occurs
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String traceId = extractOrGenerateTraceId(request);
        
        try {
            MDC.put(TRACE_ID_MDC_KEY, traceId);
            response.setHeader(TRACE_ID_HEADER, traceId);
            
            logger.debug("Processing request: {} {} with traceId: {}", 
                    request.getMethod(), request.getRequestURI(), traceId);
            
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(TRACE_ID_MDC_KEY);
        }
    }

    /**
     * Extracts trace ID from request header or generates a new one.
     *
     * @param request the HTTP request
     * @return trace ID string
     */
    private String extractOrGenerateTraceId(HttpServletRequest request) {
        String headerTraceId = request.getHeader(TRACE_ID_HEADER);
        
        if (headerTraceId != null && !headerTraceId.isBlank() 
                && headerTraceId.length() <= MAX_TRACE_ID_LENGTH
                && UUID_PATTERN.matcher(headerTraceId).matches()) {
            return headerTraceId;
        }
        
        return UUID.randomUUID().toString();
    }

    /**
     * Determines which requests should be filtered.
     * This filter applies to all requests except static resources.
     *
     * @param request the HTTP request
     * @return true if the request should be filtered
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/static/") || path.startsWith("/assets/");
    }
}
