package com.tproject.workshop.config.security;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * CORS properties configurable per environment. Fail-closed: with no origins configured, no
 * cross-origin request is allowed. QA and PRD serve only the Flutter desktop app (no browser
 * origin), so they bind an empty list; local and test list their origins explicitly.
 */
@ConfigurationProperties(prefix = "cors")
@Getter
@Setter
public class CorsProperties {

    /** Allowed origins. Empty by default so an empty list in YAML never falls back to localhost. */
    private List<String> allowedOrigins = List.of();

    /** Allowed HTTP methods. */
    private List<String> allowedMethods =
            List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS");

    /** Headers allowed in requests (explicit list; add {@code traceparent} for a web front). */
    private List<String> allowedHeaders =
            List.of("Authorization", "Content-Type", "X-API-Key", "X-Request-Id");

    /** Headers exposed in responses. */
    private List<String> exposedHeaders = List.of("X-Request-Id");

    /** Whether credentials (cookies) are allowed. Auth uses headers, so it stays off. */
    private boolean allowCredentials = false;

    /** CORS configuration cache duration (in seconds). */
    private long maxAge = 3600L;
}
