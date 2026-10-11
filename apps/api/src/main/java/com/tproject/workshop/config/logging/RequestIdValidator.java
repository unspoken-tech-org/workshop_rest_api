package com.tproject.workshop.config.logging;

import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Validates the legacy X-Request-Id correlation header before it is copied to
 * response headers, MDC and structured logs.
 *
 * <p>The value intentionally follows the HTTP token character set. This keeps
 * normal UUID/ULID and client-generated correlation IDs working while
 * rejecting whitespace, control characters and unbounded values.</p>
 */
public final class RequestIdValidator {

    public static final int MAX_LENGTH = 128;

    private static final Pattern SAFE_REQUEST_ID = Pattern.compile(
            "[!#$%&'*+\\-.^_`|~0-9A-Za-z]{1," + MAX_LENGTH + "}");

    private RequestIdValidator() {
    }

    /**
     * Returns a trimmed valid value, or a server-generated UUID for invalid
     * input. Invalid client input does not make an otherwise valid request
     * fail; it simply cannot control the correlation value used by the app.
     */
    public static String normalizeOrGenerate(String value) {
        if (value != null) {
            String normalized = value.trim();
            if (isValid(normalized)) {
                return normalized;
            }
        }
        return UUID.randomUUID().toString();
    }

    public static boolean isValid(String value) {
        return value != null
                && value.length() <= MAX_LENGTH
                && SAFE_REQUEST_ID.matcher(value).matches();
    }
}
