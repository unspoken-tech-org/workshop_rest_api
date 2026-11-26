package com.tproject.workshop.integration.support;

import static io.restassured.RestAssured.given;

import io.restassured.specification.RequestSpecification;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Instance-scoped authentication client for integration tests. Holds no static mutable state so
 * cached tokens never leak across test instances.
 */
public final class TestAuthClient {

    public static final String MOBILE_API_KEY = "sk_mobile_TEST_KEY_MOBILE_12345678901234567890";
    public static final String SERVICE_API_KEY = "sk_mobile_TEST_KEY_SERVICE_12345678901234567890";

    private static final String AUTH_ENDPOINT = "/api/auth/token";
    private static final String API_KEY_HEADER = "X-API-Key";

    private final RequestSpecification spec;
    private final Map<String, String> tokenCache = new ConcurrentHashMap<>();

    public TestAuthClient(RequestSpecification spec) {
        this.spec = Objects.requireNonNull(spec, "spec must not be null");
    }

    public String getAccessToken(String apiKey) {
        return tokenCache.computeIfAbsent(apiKey, this::fetchAccessToken);
    }

    public String getRefreshToken(String apiKey) {
        return given().spec(spec)
                .header(API_KEY_HEADER, apiKey)
                .body(
                        Map.of(
                                "boundDeviceId",
                                "test-device-refresh-" + System.currentTimeMillis(),
                                "appVersion",
                                "1.0.0-test"))
                .when()
                .post(AUTH_ENDPOINT)
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getString("refreshToken");
    }

    public void invalidateTokenCache() {
        tokenCache.clear();
    }

    private String fetchAccessToken(String apiKey) {
        return given().spec(spec)
                .header(API_KEY_HEADER, apiKey)
                .body(
                        Map.of(
                                "boundDeviceId",
                                "test-device-" + System.currentTimeMillis(),
                                "appVersion",
                                "1.0.0-test"))
                .when()
                .post(AUTH_ENDPOINT)
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getString("accessToken");
    }
}
