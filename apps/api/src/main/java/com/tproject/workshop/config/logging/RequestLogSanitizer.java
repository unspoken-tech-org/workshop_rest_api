package com.tproject.workshop.config.logging;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.HandlerMapping;

import java.util.Optional;

/**
 * Describes requests in logs by their parameterized route (for example
 * {@code /v1/customers/{id}}) instead of the concrete URI, so IDs, path
 * values and query strings never reach logs. Request bodies are never read.
 */
public final class RequestLogSanitizer {

    static final String UNMATCHED_ROUTE = "unmatched";
    static final int MAX_UNMATCHED_PATH_LENGTH = 200;

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

    /**
     * Requested path of a request that matched no route, without the query string.
     * Control characters (CR, LF, ...) are replaced so the path cannot forge log lines,
     * and runs of 5+ digits become {@code {n}}: a 401 on a real route is also unmatched,
     * so the path could carry a CPF or phone number.
     * Empty when a route matched, since the parameterized route is logged instead.
     */
    public static Optional<String> unmatchedPath(HttpServletRequest request) {
        if (!UNMATCHED_ROUTE.equals(route(request))) {
            return Optional.empty();
        }

        String path = request.getRequestURI()
                .replaceAll("\\p{Cntrl}", "_")
                .replaceAll("[0-9]{5,}", "{n}");
        return Optional.of(path.length() > MAX_UNMATCHED_PATH_LENGTH
                ? path.substring(0, MAX_UNMATCHED_PATH_LENGTH)
                : path);
    }
}
