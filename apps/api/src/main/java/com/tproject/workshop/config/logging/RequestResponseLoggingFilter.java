package com.tproject.workshop.config.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;


/**
 * Runs before the Spring Security filter chain so requests rejected there
 * (401/403) are also logged (R18); their route is {@code unmatched}, since
 * they never reach a handler.
 */
@Component
@Order(SecurityProperties.DEFAULT_FILTER_ORDER - 1)
public class RequestResponseLoggingFilter extends OncePerRequestFilter {

    private static final Logger LOG = LoggerFactory.getLogger("http.access");

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/swagger")
                || path.startsWith("/v3/api-docs")
                || path.equals("/favicon.ico");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        Instant start = Instant.now();

        try {
            chain.doFilter(request, response);
        } finally {
            long durationMs = Duration.between(start, Instant.now()).toMillis();
            // Successful Actuator calls (health checks, scrapes) are noise;
            // denied ones reveal scans and credential guessing.
            if (!isActuator(request) || response.getStatus() >= 400) {
                logRequest(request, response, durationMs);
            }
        }
    }

    private static boolean isActuator(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/actuator");
    }

    private void logRequest(HttpServletRequest request,
                            HttpServletResponse response,
                            long durationMs) {

        String method = request.getMethod();
        // Parameterized route (e.g. /v1/customers/{id}): concrete URIs carry
        // IDs and path values that must not reach logs.
        String path = RequestLogSanitizer.route(request);
        int status = response.getStatus();

        MDC.put("httpMethod", method);
        MDC.put("httpPath", path);
        MDC.put("httpStatus", String.valueOf(status));
        MDC.put("durationMs", String.valueOf(durationMs));

        try {
            boolean isError = status >= 400;
            // Query strings and bodies can contain PII, credentials or free
            // text. Access logs contain only low-risk request metadata.
            if (isError) {
                LOG.warn("HTTP {} {} -> {} ({}ms)", method, path, status, durationMs);
            } else {
                LOG.info("HTTP {} {} -> {} ({}ms)", method, path, status, durationMs);
            }
        } finally {
            // Clear extra MDC
            MDC.remove("httpMethod");
            MDC.remove("httpPath");
            MDC.remove("httpStatus");
            MDC.remove("durationMs");
        }
    }
}
