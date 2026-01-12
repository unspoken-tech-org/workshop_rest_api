package com.tproject.workshop.integration;

import io.restassured.specification.RequestSpecification;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static io.restassured.RestAssured.given;

/**
 * Helper class for managing authentication in integration tests.
 * Provides test API Keys and methods to obtain access tokens.
 */
public final class TestAuthHelper {

    private TestAuthHelper() {
        // Utility class
    }

    // Test API Keys (must match AuthSetup.sql)
    public static final String MOBILE_API_KEY = "sk_mobile_TEST_KEY_MOBILE_12345678901234567890";
    public static final String WEB_API_KEY = "sk_web_TEST_KEY_WEB_12345678901234567890";
    public static final String DESKTOP_API_KEY = "sk_desktop_TEST_KEY_DESKTOP_12345678901234567890";
    public static final String SERVER_API_KEY = "sk_server_TEST_KEY_SERVER_12345678901234567890";
    public static final String SERVICE_API_KEY = "sk_mobile_TEST_KEY_SERVICE_12345678901234567890";
    public static final String EXPIRED_API_KEY = "sk_mobile_TEST_KEY_EXPIRED_12345678901234567890";
    public static final String INACTIVE_API_KEY = "sk_mobile_TEST_KEY_INACTIVE_12345678901234567890";
    public static final String INVALID_API_KEY = "sk_mobile_INVALID_KEY_DOES_NOT_EXIST";

    private static final String AUTH_ENDPOINT = "/api/auth/token";
    private static final String API_KEY_HEADER = "X-API-Key";

    // Token cache to avoid repeated auth calls
    private static final Map<String, String> tokenCache = new ConcurrentHashMap<>();

    /**
     * Obtains an access token for the given API Key.
     * Results are cached to improve test performance.
     *
     * @param spec   the request specification
     * @param apiKey the API Key to authenticate with
     * @return the access token
     */
    public static String getAccessToken(RequestSpecification spec, String apiKey) {
        return tokenCache.computeIfAbsent(apiKey, key -> fetchAccessToken(spec, key));
    }

    /**
     * Fetches a new access token from the auth endpoint.
     */
    private static String fetchAccessToken(RequestSpecification spec, String apiKey) {
        return given()
                .spec(spec)
                .header(API_KEY_HEADER, apiKey)
                .body(Map.of(
                        "deviceId", "test-device-" + System.currentTimeMillis(),
                        "appVersion", "1.0.0-test"
                ))
                .when()
                .post(AUTH_ENDPOINT)
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getString("accessToken");
    }

    /**
     * Obtains a refresh token for the given API Key.
     *
     * @param spec   the request specification
     * @param apiKey the API Key to authenticate with
     * @return the refresh token
     */
    public static String getRefreshToken(RequestSpecification spec, String apiKey) {
        return given()
                .spec(spec)
                .header(API_KEY_HEADER, apiKey)
                .body(Map.of(
                        "deviceId", "test-device-refresh-" + System.currentTimeMillis(),
                        "appVersion", "1.0.0-test"
                ))
                .when()
                .post(AUTH_ENDPOINT)
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getString("refreshToken");
    }

    /**
     * Clears the token cache.
     * Useful when API Keys are modified during tests.
     */
    public static void invalidateTokenCache() {
        tokenCache.clear();
    }
}
