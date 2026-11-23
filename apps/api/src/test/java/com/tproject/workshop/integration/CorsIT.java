package com.tproject.workshop.integration;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * CORS preflight through the real filter chain. The test profile lists only its own origins, so any
 * other origin (including the old localhost:3000 default) gets no CORS headers, and credentials are
 * never allowed.
 */
class CorsIT extends AbstractIntegrationLiveTest {

    private static final String PATH = "/api/auth/token";

    @ParameterizedTest
    @ValueSource(strings = {"https://evil.example", "http://localhost:3000"})
    void shouldRejectPreflightFromUnlistedOrigin(String origin) {
        given().spec(SPEC)
                .header("Origin", origin)
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "X-API-Key")
                .when()
                .options(PATH)
                .then()
                .statusCode(HttpStatus.SC_FORBIDDEN)
                .header("Access-Control-Allow-Origin", nullValue())
                .header("Access-Control-Allow-Credentials", nullValue());
    }

    @Test
    void shouldAllowPreflightFromListedOriginWithoutCredentials() {
        // 127.0.0.1:8081 is cross-origin to localhost:8081 and is listed in application-test.yml.
        given().spec(SPEC)
                .header("Origin", "http://127.0.0.1:8081")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "X-API-Key, Content-Type")
                .when()
                .options(PATH)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .header("Access-Control-Allow-Origin", equalTo("http://127.0.0.1:8081"))
                .header("Access-Control-Allow-Headers", containsStringIgnoringCase("X-API-Key"))
                .header("Access-Control-Allow-Credentials", nullValue());
    }
}
