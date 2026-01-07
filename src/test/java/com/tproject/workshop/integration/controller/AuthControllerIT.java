package com.tproject.workshop.integration.controller;

import com.tproject.workshop.integration.AbstractIntegrationLiveTest;
import com.tproject.workshop.integration.TestAuthHelper;
import io.jsonwebtoken.Jwts;
import io.restassured.response.Response;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.context.jdbc.Sql;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Sql({"/test-scripts/cleanTestData.sql", "/test-scripts/AuthSetup.sql", "/test-scripts/resetTablesSequence.sql"})
public class AuthControllerIT extends AbstractIntegrationLiveTest {

    private static final String AUTH_PATH = "/api/auth";
    private static final String API_KEY_HEADER = "X-API-Key";

    @BeforeEach
    void setUp() {
        TestAuthHelper.invalidateTokenCache();
    }

    // ========== Token Endpoint Tests ==========

    @Order(1)
    @DisplayName("Get token with valid API Key")
    @MethodSource("validApiKeyArguments")
    @ParameterizedTest(name = "{displayName} : {0} platform {1}")
    public void shouldReturnTokens_whenValidApiKey(int index, String platform, String apiKey) {
        Response response = given().spec(SPEC)
                .header(API_KEY_HEADER, apiKey)
                .body(Map.of(
                        "deviceId", "test-device-" + platform,
                        "appVersion", "1.0.0"
                ))
                .when()
                .post(AUTH_PATH + "/token")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .extract()
                .response();

        super.validateResponseIgnoreAttributes(index, response, List.of(
                "accessToken", "refreshToken"
        ));
    }

    private static Stream<Arguments> validApiKeyArguments() {
        return Stream.of(
                Arguments.of(1, "MOBILE", TestAuthHelper.MOBILE_API_KEY),
                Arguments.of(2, "WEB", TestAuthHelper.WEB_API_KEY),
                Arguments.of(3, "DESKTOP", TestAuthHelper.DESKTOP_API_KEY),
                Arguments.of(4, "SERVER", TestAuthHelper.SERVER_API_KEY)
        );
    }

    @Order(2)
    @DisplayName("Reject token with invalid API Key")
    @Test
    public void shouldReturn401_whenInvalidApiKey() {
        Response response = given().spec(SPEC)
                .header(API_KEY_HEADER, TestAuthHelper.INVALID_API_KEY)
                .body(Map.of(
                        "deviceId", "test-device",
                        "appVersion", "1.0.0"
                ))
                .when()
                .post(AUTH_PATH + "/token")
                .then()
                .statusCode(HttpStatus.SC_UNAUTHORIZED)
                .extract()
                .response();

        super.validateResponse("invalid_key", response);
    }

    @Order(3)
    @DisplayName("Reject token without API Key header")
    @Test
    public void shouldReturn400_whenNoApiKeyHeader() {
        given().spec(SPEC)
                .body(Map.of(
                        "deviceId", "test-device",
                        "appVersion", "1.0.0"
                ))
                .when()
                .post(AUTH_PATH + "/token")
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST);
    }

    @Order(4)
    @DisplayName("Reject token with expired API Key")
    @Test
    public void shouldReturn401_whenExpiredApiKey() {
        Response response = given().spec(SPEC)
                .header(API_KEY_HEADER, TestAuthHelper.EXPIRED_API_KEY)
                .body(Map.of(
                        "deviceId", "test-device",
                        "appVersion", "1.0.0"
                ))
                .when()
                .post(AUTH_PATH + "/token")
                .then()
                .statusCode(HttpStatus.SC_UNAUTHORIZED)
                .extract()
                .response();

        super.validateResponse("expired_key", response);
    }

    @Order(5)
    @DisplayName("Reject token with inactive API Key")
    @Test
    public void shouldReturn401_whenInactiveApiKey() {
        Response response = given().spec(SPEC)
                .header(API_KEY_HEADER, TestAuthHelper.INACTIVE_API_KEY)
                .body(Map.of(
                        "deviceId", "test-device",
                        "appVersion", "1.0.0"
                ))
                .when()
                .post(AUTH_PATH + "/token")
                .then()
                .statusCode(HttpStatus.SC_UNAUTHORIZED)
                .extract()
                .response();

        super.validateResponse("inactive_key", response);
    }

    // ========== Refresh Token Endpoint Tests ==========

    @Order(6)
    @DisplayName("Refresh token with valid refresh token")
    @Test
    public void shouldReturnNewAccessToken_whenValidRefreshToken() {
        // First get a refresh token
        String refreshToken = TestAuthHelper.getRefreshToken(SPEC, TestAuthHelper.MOBILE_API_KEY);

        Response response = given().spec(SPEC)
                .body(Map.of("refreshToken", refreshToken))
                .when()
                .post(AUTH_PATH + "/refresh")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .extract()
                .response();

        super.validateResponseIgnoreAttributes("refresh_success", response, List.of("accessToken"));
    }

    @Order(7)
    @DisplayName("Reject refresh with invalid refresh token")
    @Test
    public void shouldReturn401_whenInvalidRefreshToken() {
        Response response = given().spec(SPEC)
                .body(Map.of("refreshToken", "invalid_refresh_token_12345"))
                .when()
                .post(AUTH_PATH + "/refresh")
                .then()
                .statusCode(HttpStatus.SC_UNAUTHORIZED)
                .extract()
                .response();

        super.validateResponse("invalid_refresh", response);
    }

    @Order(8)
    @DisplayName("Reject refresh with expired refresh token (R2)")
    @Test
    public void shouldReturn401_whenExpiredRefreshToken() {
        // The token 'rt_EXPIRED_TOKEN_1234567890' is inserted via AuthSetup.sql with an expired date
        Response response = given().spec(SPEC)
                .body(Map.of("refreshToken", "rt_EXPIRED_TOKEN_1234567890"))
                .when()
                .post(AUTH_PATH + "/refresh")
                .then()
                .statusCode(HttpStatus.SC_UNAUTHORIZED)
                .extract()
                .response();

        super.validateResponse("expired_refresh", response);
    }

    // ========== Revoke Token Endpoint Tests ==========

    @Order(9)
    @DisplayName("Revoke refresh token successfully")
    @Test
    public void shouldReturn204_whenRevokingValidToken() {
        // First get a refresh token
        String refreshToken = TestAuthHelper.getRefreshToken(SPEC, TestAuthHelper.MOBILE_API_KEY);

        given().spec(SPEC)
                .body(Map.of("refreshToken", refreshToken))
                .when()
                .post(AUTH_PATH + "/revoke")
                .then()
                .statusCode(HttpStatus.SC_NO_CONTENT);

        // Try to use the revoked token - should fail
        given().spec(SPEC)
                .body(Map.of("refreshToken", refreshToken))
                .when()
                .post(AUTH_PATH + "/refresh")
                .then()
                .statusCode(HttpStatus.SC_UNAUTHORIZED);
    }

    // ========== Protected Endpoint Tests ==========

    @Order(10)
    @DisplayName("Access protected endpoint with valid JWT")
    @Test
    public void shouldReturn200_whenAccessingProtectedEndpointWithValidJwt() {
        // Use authenticated spec to access a protected endpoint
        given().spec(getAuthenticatedSpec())
                .when()
                .get("/v1/admin/api-keys")
                .then()
                .statusCode(HttpStatus.SC_OK);
    }

    @Order(11)
    @DisplayName("Reject access to protected endpoint without JWT")
    @Test
    public void shouldReturn401_whenAccessingProtectedEndpointWithoutJwt() {
        given().spec(SPEC)
                .when()
                .get("/v1/admin/api-keys")
                .then()
                .statusCode(HttpStatus.SC_UNAUTHORIZED);
    }

    @Order(12)
    @DisplayName("Reject access to protected endpoint with invalid JWT")
    @Test
    public void shouldReturn401_whenAccessingProtectedEndpointWithInvalidJwt() {
        given().spec(SPEC)
                .header("Authorization", "Bearer invalid.jwt.token")
                .when()
                .get("/v1/admin/api-keys")
                .then()
                .statusCode(HttpStatus.SC_UNAUTHORIZED);
    }

    @Order(13)
    @DisplayName("Reject access with JWT signed by a different RSA key (Key Rotation Scenario)")
    @Test
    public void shouldReturn401_whenJwtSignedByDifferentKey() throws NoSuchAlgorithmException {
        // 1. Generate a completely different RSA KeyPair for this test
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        KeyPair differentKeyPair = keyPairGenerator.generateKeyPair();

        // 2. Create a JWT signed with this "intruder" private key
        Instant now = Instant.now();
        String intruderToken = Jwts.builder()
                .header().type("JWT").and()
                .subject("Test-Client")
                .issuer("workshop-api")
                .audience().add("workshop-api").and()
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(900)))
                .id(UUID.randomUUID().toString())
                .signWith(differentKeyPair.getPrivate(), Jwts.SIG.RS256)
                .compact();

        // 3. Try to access a protected endpoint with this token
        // The server will try to validate it using its own public key and fail
        given().spec(SPEC)
                .header("Authorization", "Bearer " + intruderToken)
                .when()
                .get("/v1/admin/api-keys")
                .then()
                .statusCode(HttpStatus.SC_UNAUTHORIZED);
    }
}
