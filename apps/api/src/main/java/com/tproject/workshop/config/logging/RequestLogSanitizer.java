package com.tproject.workshop.config.logging;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.HandlerMapping;

/**
 * Describes requests in logs by their parameterized route (for example
 * {@code /v1/customers/{id}}) instead of the concrete URI, so IDs, path
 * values and query strings never reach logs. Request bodies are never read.
 */
public final class RequestLogSanitizer {

    static final String UNMATCHED_ROUTE = "unmatched";

    private RequestLogSanitizer() {
    }

    public static String route(WebRequest request) {
        if (request == null) {
            return "route=" + UNMATCHED_ROUTE;
        }

        Object pattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
        return "route=" + (pattern instanceof String value ? value : UNMATCHED_ROUTE);
    }

    public static String route(HttpServletRequest request) {
        Object pattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        return pattern instanceof String value ? value : UNMATCHED_ROUTE;
    }
}
