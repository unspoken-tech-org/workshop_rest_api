package com.tproject.workshop.config.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * CORS properties configurable per environment.
 * Allows different origins in dev, test, and production.
 */
@ConfigurationProperties(prefix = "cors")
@Getter
@Setter
public class CorsProperties {

    /**
     * List of allowed origins for CORS requests.
     */
    private List<String> allowedOrigins = List.of("http://localhost:3000");

    /**
     * Allowed HTTP methods.
     */
    private List<String> allowedMethods = List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS");

    /**
     * Headers allowed in requests.
     */
    private List<String> allowedHeaders = List.of("*");

    /**
     * Headers exposed in responses.
     */
    private List<String> exposedHeaders = List.of("X-Request-Id");

    /**
     * Whether credentials (cookies, auth headers) are allowed.
     */
    private boolean allowCredentials = true;

    /**
     * CORS configuration cache duration (in seconds).
     */
    private long maxAge = 3600L;
}
