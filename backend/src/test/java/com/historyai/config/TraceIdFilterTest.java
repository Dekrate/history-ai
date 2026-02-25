package com.historyai.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TraceIdFilter.
 */
@ExtendWith(MockitoExtension.class)
class TraceIdFilterTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private TraceIdFilter filter;

    @BeforeEach
    void setUp() {
        filter = new TraceIdFilter();
        MDC.clear();
    }

    @Test
    void doFilterInternal_ShouldGenerateNewTraceId_WhenNoHeader() throws Exception {
        when(request.getHeader("X-Trace-Id")).thenReturn(null);
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/characters");

        filter.doFilterInternal(request, response, filterChain);

        verify(response).setHeader(eq("X-Trace-Id"), anyString());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_ShouldUseHeaderTraceId_WhenPresent() throws Exception {
        String existingTraceId = UUID.randomUUID().toString();
        when(request.getHeader("X-Trace-Id")).thenReturn(existingTraceId);
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/characters");

        filter.doFilterInternal(request, response, filterChain);

        verify(response).setHeader("X-Trace-Id", existingTraceId);
    }

    @Test
    void doFilterInternal_ShouldRemoveMdcAfterProcessing() throws Exception {
        when(request.getHeader("X-Trace-Id")).thenReturn(null);
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/characters");

        filter.doFilterInternal(request, response, filterChain);

        assertNull(MDC.get("traceId"));
    }

    @Test
    void doFilterInternal_ShouldAddTraceIdToMdcDuringProcessing() throws Exception {
        final String[] capturedTraceId = new String[1];
        
        when(request.getHeader("X-Trace-Id")).thenReturn(null);
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/characters");
        
        doAnswer(invocation -> {
            capturedTraceId[0] = MDC.get("traceId");
            return null;
        }).when(filterChain).doFilter(any(), any());
        
        filter.doFilterInternal(request, response, filterChain);

        assertNotNull(capturedTraceId[0]);
    }

    @Test
    void shouldNotFilter_WhenStaticResource_ShouldReturnTrue() {
        when(request.getRequestURI()).thenReturn("/static/css/style.css");

        boolean result = filter.shouldNotFilter(request);

        assertTrue(result);
    }

    @Test
    void shouldNotFilter_WhenAssetsResource_ShouldReturnTrue() {
        when(request.getRequestURI()).thenReturn("/assets/image.png");

        boolean result = filter.shouldNotFilter(request);

        assertTrue(result);
    }

    @Test
    void shouldNotFilter_WhenApiRequest_ShouldReturnFalse() {
        when(request.getRequestURI()).thenReturn("/api/characters");

        boolean result = filter.shouldNotFilter(request);

        assertFalse(result);
    }

    @Test
    void doFilterInternal_ShouldCatchExceptions() throws Exception {
        when(request.getHeader("X-Trace-Id")).thenReturn(null);
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/characters");
        doThrow(new RuntimeException("Test exception")).when(filterChain).doFilter(request, response);

        assertThrows(RuntimeException.class, 
                () -> filter.doFilterInternal(request, response, filterChain));
        
        assertNull(MDC.get("traceId"));
    }
}
