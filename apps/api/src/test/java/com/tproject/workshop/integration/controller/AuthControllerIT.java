package com.tproject.workshop.integration.controller;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.startsWith;

import com.tproject.workshop.config.security.RsaKeyProperties;
import com.tproject.workshop.integration.support.AbstractIntegrationTest;
import com.tproject.workshop.support.fixtures.IntegrationFixtures;
import com.tproject.workshop.util.KeyGeneratorUtils;
import io.jsonwebtoken.Jwts;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;

class AuthControllerIT extends AbstractIntegrationTest {

    private static final String AUTH_PATH = "/api/auth";
    private static final String API_KEY_HEADER = "X-API-Key";

    private static final String MOBILE_API_KEY = "sk_mobile_TEST_KEY_MOBILE_12345678901234567890";
    private static final String MOBILE_API_KEY_SECOND_USER =
            "sk_mobile_TEST_KEY_MOBILE_2_12345678901234567890";
    private static final String WEB_API_KEY = "sk_web_TEST_KEY_WEB_12345678901234567890";
    private static final String DESKTOP_API_KEY =
            "sk_desktop_TEST_KEY_DESKTOP_12345678901234567890";
    private static final String SERVER_API_KEY = "sk_server_TEST_KEY_SERVER_12345678901234567890";
    private static final String SERVICE_API_KEY = "sk_mobile_TEST_KEY_SERVICE_12345678901234567890";
    private static final String EXPIRED_API_KEY = "sk_mobile_TEST_KEY_EXPIRED_12345678901234567890";
    private static final String INACTIVE_API_KEY =
            "sk_mobile_TEST_KEY_INACTIVE_12345678901234567890";
    private static final String INVALID_API_KEY = "sk_mobile_INVALID_KEY_DOES_NOT_EXIST";
    private static final String EXPIRED_REFRESH_TOKEN = "rt_EXPIRED_TOKEN_1234567890";

    @Autowired private RsaKeyProperties rsaKeyProperties;

    @Autowired private ApplicationContext context;

    private long adminId;
    private long secondAdminId;
    private long serviceId;

    @BeforeEach
    void seedApiKeys() {
        IntegrationFixtures fixtures = new IntegrationFixtures(jdbcTemplate, context);
        adminId =
                fixtures.apiKey(
                        MOBILE_API_KEY, "test_app", "admin_user", "MOBILE", "ADMIN", true, null);
        fixtures.apiKey(WEB_API_KEY, "test_app", "admin_user", "WEB", "ADMIN", true, null);
        fixtures.apiKey(DESKTOP_API_KEY, "test_app", "admin_user", "DESKTOP", "ADMIN", true, null);
        fixtures.apiKey(SERVER_API_KEY, "test_app", "admin_user", "SERVER", "ADMIN", true, null);
        secondAdminId =
                fixtures.apiKey(
                        MOBILE_API_KEY_SECOND_USER,
                        "test_app",
                        "second_user",
                        "MOBILE",
                        "ADMIN",
                        true,
                        null);
        serviceId =
                fixtures.apiKey(
                        SERVICE_API_KEY,
                        "test_service_app",
                        "service_user",
                        "MOBILE",
                        "SERVICE",
                        true,
                        null);
        fixtures.apiKey(
                EXPIRED_API_KEY,
                "test_expired",
                "expired_user",
                "MOBILE",
                "SERVICE",
                true,
                Instant.now().minusSeconds(86_400));
        fixtures.apiKey(
                INACTIVE_API_KEY,
                "test_inactive",
                "inactive_user",
                "MOBILE",
                "SERVICE",
                false,
                null);
        fixtures.expiredRefreshToken(EXPIRED_REFRESH_TOKEN, adminId);
    }

    // ========== Token Endpoint Tests ==========

    @DisplayName("Get token with valid API Key")
    @MethodSource("validApiKeyArguments")
    @ParameterizedTest(name = "{displayName} : platform {0}")
    void shouldReturnTokens_whenValidApiKey(String platform, String apiKey) {
        Response response =
                given().spec(SPEC)
                        .header(API_KEY_HEADER, apiKey)
                        .body(
                                Map.of(
                                        "boundDeviceId",
                                        "test-device-" + platform,
                                        "appVersion",
                                        "1.0.0"))
                        .when()
                        .post(AUTH_PATH + "/token")
                        .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract()
                        .response();

        assertTokenResponse(response, true);
    }

    private static Stream<Arguments> validApiKeyArguments() {
        return Stream.of(
                Arguments.of("MOBILE", MOBILE_API_KEY),
                Arguments.of("WEB", WEB_API_KEY),
                Arguments.of("DESKTOP", DESKTOP_API_KEY),
                Arguments.of("SERVER", SERVER_API_KEY));
    }

    @DisplayName("Reject token with invalid API Key")
    @Test
    void shouldReturn401_whenInvalidApiKey() {
        Response response =
                given().spec(SPEC)
                        .header(API_KEY_HEADER, INVALID_API_KEY)
                        .body(
                                Map.of(
                                        "boundDeviceId", "test-device",
                                        "appVersion", "1.0.0"))
                        .when()
                        .post(AUTH_PATH + "/token")
                        .then()
                        .statusCode(HttpStatus.UNAUTHORIZED.value())
                        .extract()
                        .response();

        assertProblem(
                response,
                401,
                "API Key Inválida",
                "Invalid API Key",
                "/api/auth/token",
                "auth.invalid.api.key");
    }

    @DisplayName("Reject token without API Key header")
    @Test
    void shouldReturn400_whenNoApiKeyHeader() {
        given().spec(SPEC)
                .body(
                        Map.of(
                                "boundDeviceId", "test-device",
                                "appVersion", "1.0.0"))
                .when()
                .post(AUTH_PATH + "/token")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @DisplayName("Reject token with expired API Key")
    @Test
    void shouldReturn401_whenExpiredApiKey() {
        Response response =
                given().spec(SPEC)
                        .header(API_KEY_HEADER, EXPIRED_API_KEY)
                        .body(
                                Map.of(
                                        "boundDeviceId", "test-device",
                                        "appVersion", "1.0.0"))
                        .when()
                        .post(AUTH_PATH + "/token")
                        .then()
                        .statusCode(HttpStatus.UNAUTHORIZED.value())
                        .extract()
                        .response();

        assertProblem(
                response,
                401,
                "API Key Inválida",
                "Expired API Key",
                "/api/auth/token",
                "auth.invalid.api.key");
    }

    @DisplayName("Reject token with inactive API Key")
    @Test
    void shouldReturn401_whenInactiveApiKey() {
        Response response =
                given().spec(SPEC)
                        .header(API_KEY_HEADER, INACTIVE_API_KEY)
                        .body(
                                Map.of(
                                        "boundDeviceId", "test-device",
                                        "appVersion", "1.0.0"))
                        .when()
                        .post(AUTH_PATH + "/token")
                        .then()
                        .statusCode(HttpStatus.UNAUTHORIZED.value())
                        .extract()
                        .response();

        assertProblem(
                response,
                401,
                "API Key Inválida",
                "Invalid API Key",
                "/api/auth/token",
                "auth.invalid.api.key");
    }

    // ========== Refresh Token Endpoint Tests ==========

    @DisplayName("Refresh token with valid refresh token")
    @Test
    void shouldReturnNewAccessToken_whenValidRefreshToken() {
        // First get a refresh token
        String refreshToken = authClient.getRefreshToken(MOBILE_API_KEY);

        Response response =
                given().spec(SPEC)
                        .body(Map.of("refreshToken", refreshToken))
                        .when()
                        .post(AUTH_PATH + "/refresh")
                        .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract()
                        .response();

        assertTokenResponse(response, false);
    }

    @DisplayName("Reject refresh with invalid refresh token")
    @Test
    void shouldReturn401_whenInvalidRefreshToken() {
        Response response =
                given().spec(SPEC)
                        .body(Map.of("refreshToken", "invalid_refresh_token_12345"))
                        .when()
                        .post(AUTH_PATH + "/refresh")
                        .then()
                        .statusCode(HttpStatus.UNAUTHORIZED.value())
                        .extract()
                        .response();

        assertProblem(
                response,
                401,
                "Token Inválido",
                "Refresh token not found",
                "/api/auth/refresh",
                "auth.invalid.token");
    }

    @DisplayName("Reject refresh with expired refresh token (R2)")
    @Test
    void shouldReturn401_whenExpiredRefreshToken() {
        // The token 'rt_EXPIRED_TOKEN_1234567890' is inserted via AuthSetup.sql with an expired
        // date
        Response response =
                given().spec(SPEC)
                        .body(Map.of("refreshToken", EXPIRED_REFRESH_TOKEN))
                        .when()
                        .post(AUTH_PATH + "/refresh")
                        .then()
                        .statusCode(HttpStatus.UNAUTHORIZED.value())
                        .extract()
                        .response();

        assertProblem(
                response,
                401,
                "Token Inválido",
                "Refresh token expired",
                "/api/auth/refresh",
                "auth.invalid.token");
    }

    // ========== Revoke Token Endpoint Tests ==========

    @DisplayName("Revoke refresh token successfully")
    @Test
    void shouldReturn204_whenRevokingValidToken() {
        // First get a refresh token
        String refreshToken = authClient.getRefreshToken(MOBILE_API_KEY);

        given().spec(SPEC)
                .body(Map.of("refreshToken", refreshToken))
                .when()
                .post(AUTH_PATH + "/revoke")
                .then()
                .statusCode(HttpStatus.NO_CONTENT.value());

        // Try to use the revoked token - should fail
        given().spec(SPEC)
                .body(Map.of("refreshToken", refreshToken))
                .when()
                .post(AUTH_PATH + "/refresh")
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @DisplayName("Refresh tokens are scoped to API Key")
    @Test
    void shouldAllowRefreshTokensForDifferentUsersOnSameDevice() {
        String boundDeviceId = "shared-device-id";

        String firstUserRefreshToken =
                given().spec(SPEC)
                        .header(API_KEY_HEADER, MOBILE_API_KEY)
                        .body(Map.of("boundDeviceId", boundDeviceId, "appVersion", "1.0.0"))
                        .when()
                        .post(AUTH_PATH + "/token")
                        .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract()
                        .jsonPath()
                        .getString("refreshToken");

        given().spec(SPEC)
                .header(API_KEY_HEADER, MOBILE_API_KEY_SECOND_USER)
                .body(Map.of("boundDeviceId", boundDeviceId, "appVersion", "1.0.0"))
                .when()
                .post(AUTH_PATH + "/token")
                .then()
                .statusCode(HttpStatus.OK.value());

        given().spec(SPEC)
                .body(Map.of("refreshToken", firstUserRefreshToken))
                .when()
                .post(AUTH_PATH + "/refresh")
                .then()
                .statusCode(HttpStatus.OK.value());
    }

    // ========== Protected Endpoint Tests ==========

    @DisplayName("Access protected endpoint with valid JWT")
    @Test
    void shouldReturn200_whenAccessingProtectedEndpointWithValidJwt() {
        // Use authenticated spec to access a protected endpoint
        given().spec(getAuthenticatedSpec())
                .when()
                .get("/v1/admin/api-keys")
                .then()
                .statusCode(HttpStatus.OK.value());
    }

    @DisplayName("Reject access to protected endpoint without JWT")
    @Test
    void shouldReturn401_whenAccessingProtectedEndpointWithoutJwt() {
        given().spec(SPEC)
                .when()
                .get("/v1/admin/api-keys")
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @DisplayName("Reject access to protected endpoint with invalid JWT")
    @Test
    void shouldReturn401_whenAccessingProtectedEndpointWithInvalidJwt() {
        given().spec(SPEC)
                .header("Authorization", "Bearer invalid.jwt.token")
                .when()
                .get("/v1/admin/api-keys")
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @DisplayName("Reject access with JWT signed by a different RSA key (Key Rotation Scenario)")
    @Test
    void shouldReturn401_whenJwtSignedByDifferentKey() throws NoSuchAlgorithmException {
        // 1. Generate a completely different RSA KeyPair for this test
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        KeyPair differentKeyPair = keyPairGenerator.generateKeyPair();

        // 2. Create a JWT signed with this "intruder" private key
        Instant now = Instant.now();
        String intruderToken =
                Jwts.builder()
                        .header()
                        .type("JWT")
                        .and()
                        .subject("Test-Client")
                        .issuer("workshop-api")
                        .audience()
                        .add("workshop-api")
                        .and()
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
                .statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @DisplayName("Reject token when API Key is bound to another device")
    @Test
    void shouldReturn409_whenApiKeyBoundToAnotherDevice() {
        given().spec(SPEC)
                .header(API_KEY_HEADER, SERVICE_API_KEY)
                .body(
                        Map.of(
                                "boundDeviceId", "device-a",
                                "appVersion", "1.0.0"))
                .when()
                .post(AUTH_PATH + "/token")
                .then()
                .statusCode(HttpStatus.OK.value());

        given().spec(SPEC)
                .header(API_KEY_HEADER, SERVICE_API_KEY)
                .body(
                        Map.of(
                                "boundDeviceId", "device-b",
                                "appVersion", "1.0.0"))
                .when()
                .post(AUTH_PATH + "/token")
                .then()
                .statusCode(HttpStatus.CONFLICT.value());
    }

    // ========== Security Hardening Tests (2.4) ==========

    @DisplayName("Rotate refresh token and revoke family on reuse")
    @Test
    void shouldRotateRefreshTokenAndRevokeAllOnReuse() {
        String initialRefreshToken = authClient.getRefreshToken(MOBILE_API_KEY);

        String rotatedRefreshToken =
                given().spec(SPEC)
                        .body(Map.of("refreshToken", initialRefreshToken))
                        .when()
                        .post(AUTH_PATH + "/refresh")
                        .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract()
                        .jsonPath()
                        .getString("refreshToken");

        Assertions.assertNotNull(rotatedRefreshToken);
        Assertions.assertNotEquals(initialRefreshToken, rotatedRefreshToken);

        // Reusing the already-rotated token must fail with 401 and revoke the rotated token too
        given().spec(SPEC)
                .body(Map.of("refreshToken", initialRefreshToken))
                .when()
                .post(AUTH_PATH + "/refresh")
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value());

        given().spec(SPEC)
                .body(Map.of("refreshToken", rotatedRefreshToken))
                .when()
                .post(AUTH_PATH + "/refresh")
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @DisplayName("Reject refresh when backing API Key is expired")
    @Test
    void shouldReturn401_whenRefreshingWithExpiredApiKey() {
        String refreshToken = authClient.getRefreshToken(SERVICE_API_KEY);

        jdbcTemplate.update(
                "UPDATE api_keys SET expires_at = CURRENT_TIMESTAMP - INTERVAL '1 minute' WHERE id = "
                        + serviceId);

        given().spec(SPEC)
                .body(Map.of("refreshToken", refreshToken))
                .when()
                .post(AUTH_PATH + "/refresh")
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @DisplayName(
            "Immediately reject access token when API Key is revoked, expired, or device-reset")
    @Test
    void shouldReturn401Immediately_whenApiKeyRevokedOrExpiredOrReset() {
        String serviceToken = authClient.getAccessToken(SERVICE_API_KEY);

        given().spec(SPEC)
                .header("Authorization", "Bearer " + serviceToken)
                .when()
                .get("/v1/colors")
                .then()
                .statusCode(HttpStatus.OK.value());

        // Revoke API Key 7 (SERVICE) -> access token must immediately fail with 401
        given().spec(getAuthenticatedSpec())
                .when()
                .delete("/v1/admin/api-keys/" + serviceId)
                .then()
                .statusCode(HttpStatus.NO_CONTENT.value());

        given().spec(SPEC)
                .header("Authorization", "Bearer " + serviceToken)
                .when()
                .get("/v1/colors")
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value());

        // Device reset on API Key 8 -> access token must immediately fail with 401
        String secondAdminToken = authClient.getAccessToken(MOBILE_API_KEY_SECOND_USER);
        given().spec(getAuthenticatedSpec())
                .when()
                .post("/v1/admin/api-keys/" + secondAdminId + "/reset-device")
                .then()
                .statusCode(HttpStatus.NO_CONTENT.value());

        given().spec(SPEC)
                .header("Authorization", "Bearer " + secondAdminToken)
                .when()
                .get("/v1/colors")
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value());

        // Expire API Key 1 -> access token must immediately fail with 401
        String adminToken = authClient.getAccessToken(MOBILE_API_KEY);
        jdbcTemplate.update(
                "UPDATE api_keys SET expires_at = CURRENT_TIMESTAMP - INTERVAL '1 minute' WHERE id = "
                        + adminId);

        given().spec(SPEC)
                .header("Authorization", "Bearer " + adminToken)
                .when()
                .get("/v1/colors")
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @DisplayName("Enforce role-based authorization across business and admin endpoints")
    @Test
    void shouldEnforceRoleAuthorizationAcrossControllers() {
        // SERVICE role can access business endpoints used by Flutter
        given().spec(getServiceAuthenticatedSpec())
                .when()
                .get("/v1/colors")
                .then()
                .statusCode(HttpStatus.OK.value());

        // SERVICE role cannot access admin endpoints
        given().spec(getServiceAuthenticatedSpec())
                .when()
                .get("/v1/admin/api-keys")
                .then()
                .statusCode(HttpStatus.FORBIDDEN.value());

        // A valid JWT bound to an active key but carrying an unauthorized role gets 403 on business
        // endpoints
        jdbcTemplate.update(
                "UPDATE api_keys SET bound_device_id = ? WHERE id = " + serviceId, "guest-device");
        Instant now = Instant.now();
        String guestToken =
                Jwts.builder()
                        .header()
                        .type("JWT")
                        .and()
                        .subject("test_service_app")
                        .issuer("workshop-api")
                        .audience()
                        .add("workshop-api")
                        .and()
                        .issuedAt(Date.from(now))
                        .expiration(Date.from(now.plusSeconds(300)))
                        .id(UUID.randomUUID().toString())
                        .claim("apiKeyId", serviceId)
                        .claim("boundDeviceId", "guest-device")
                        .claim("tv", currentTokenVersion(serviceId))
                        .claim("roles", List.of("ROLE_GUEST"))
                        .signWith(rsaKeyProperties.privateKey(), Jwts.SIG.RS256)
                        .compact();

        given().spec(SPEC)
                .header("Authorization", "Bearer " + guestToken)
                .when()
                .get("/v1/colors")
                .then()
                .statusCode(HttpStatus.FORBIDDEN.value());
    }

    @DisplayName("Store SHA-256 hashes in DB instead of plaintext API keys and refresh tokens")
    @Test
    void shouldStoreSha256HashesInDatabase() {
        Response createResponse =
                given().spec(getAuthenticatedSpec())
                        .body(
                                Map.of(
                                        "clientName", "hash_check_client",
                                        "userIdentifier", "hash_user",
                                        "platform", "MOBILE",
                                        "description", "Hash verification"))
                        .when()
                        .post("/v1/admin/api-keys")
                        .then()
                        .statusCode(HttpStatus.CREATED.value())
                        .extract()
                        .response();

        Long createdId = createResponse.jsonPath().getLong("id");
        String plaintextKey = createResponse.jsonPath().getString("keyValue");
        Assertions.assertTrue(plaintextKey.startsWith("sk_mobile_"));

        String storedKeyValue =
                jdbcTemplate.queryForObject(
                        "SELECT key_value FROM api_keys WHERE id = ?", String.class, createdId);
        Assertions.assertEquals(KeyGeneratorUtils.sha256Hex(plaintextKey), storedKeyValue);
        Assertions.assertNotEquals(plaintextKey, storedKeyValue);

        String maskedKey =
                given().spec(getAuthenticatedSpec())
                        .when()
                        .get("/v1/admin/api-keys/" + createdId)
                        .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract()
                        .jsonPath()
                        .getString("maskedKeyValue");
        Assertions.assertEquals("****", maskedKey);

        String plaintextRefresh = authClient.getRefreshToken(plaintextKey);
        String storedRefreshHash =
                jdbcTemplate.queryForObject(
                        "SELECT token FROM refresh_tokens WHERE api_key_id = ? AND revoked = false",
                        String.class,
                        createdId);
        Assertions.assertEquals(KeyGeneratorUtils.sha256Hex(plaintextRefresh), storedRefreshHash);
        Assertions.assertNotEquals(plaintextRefresh, storedRefreshHash);
    }

    // ========== Input Size Limits ==========

    @DisplayName("Reject oversized auth fields with 400")
    @MethodSource("oversizedAuthFieldArguments")
    @ParameterizedTest(name = "{displayName} : {0} {1}")
    void shouldReturn400_whenAuthFieldExceedsMaxSize(
            String path, String field, Map<String, Object> body) {
        given().spec(SPEC)
                .header(API_KEY_HEADER, MOBILE_API_KEY)
                .body(body)
                .when()
                .post(AUTH_PATH + path)
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("status", equalTo(400))
                .body("title", equalTo("Erro de Validação"))
                .body("code", equalTo("erro.validacao"))
                .body("detail", startsWith(field + " deve ter no máximo "));

        Integer boundKeys =
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM api_keys WHERE bound_device_id IS NOT NULL",
                        Integer.class);
        Assertions.assertEquals(0, boundKeys);
    }

    private static Stream<Arguments> oversizedAuthFieldArguments() {
        return Stream.of(
                Arguments.of(
                        "/token",
                        "boundDeviceId",
                        Map.of("boundDeviceId", "d".repeat(101), "appVersion", "1.0.0")),
                Arguments.of(
                        "/token",
                        "appVersion",
                        Map.of("boundDeviceId", "test-device", "appVersion", "1".repeat(51))),
                Arguments.of("/refresh", "refreshToken", Map.of("refreshToken", "r".repeat(257))),
                Arguments.of("/revoke", "refreshToken", Map.of("refreshToken", "r".repeat(257))));
    }

    // ========== Access Token Revocation (token_version) ==========

    @DisplayName("Logout immediately revokes the access token; a new login works")
    @Test
    void shouldReturn401ForAccessToken_afterLogout() {
        Map<String, String> login1 = login(MOBILE_API_KEY, "revoke-device");
        assertProtectedStatus(login1.get("accessToken"), HttpStatus.OK.value());

        given().spec(SPEC)
                .body(Map.of("refreshToken", login1.get("refreshToken")))
                .when()
                .post(AUTH_PATH + "/revoke")
                .then()
                .statusCode(HttpStatus.NO_CONTENT.value());

        assertProtectedStatus(login1.get("accessToken"), HttpStatus.UNAUTHORIZED.value());

        Map<String, String> login2 = login(MOBILE_API_KEY, "revoke-device");
        assertProtectedStatus(login2.get("accessToken"), HttpStatus.OK.value());
        assertProtectedStatus(login1.get("accessToken"), HttpStatus.UNAUTHORIZED.value());
        Assertions.assertEquals(3L, currentTokenVersion(adminId));
    }

    @DisplayName("A new login revokes access tokens from the previous login")
    @Test
    void shouldReturn401ForOldAccessToken_afterNewLogin() {
        Map<String, String> login1 = login(MOBILE_API_KEY, "relogin-device");
        Map<String, String> login2 = login(MOBILE_API_KEY, "relogin-device");

        assertProtectedStatus(login1.get("accessToken"), HttpStatus.UNAUTHORIZED.value());
        assertProtectedStatus(login2.get("accessToken"), HttpStatus.OK.value());
    }

    @DisplayName("Refresh token reuse revokes access tokens of the API Key")
    @Test
    void shouldReturn401ForAccessTokens_afterRefreshTokenReuse() {
        Map<String, String> login = login(MOBILE_API_KEY, "reuse-device");

        JsonPath refreshedJson =
                given().spec(SPEC)
                        .body(Map.of("refreshToken", login.get("refreshToken")))
                        .when()
                        .post(AUTH_PATH + "/refresh")
                        .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract()
                        .jsonPath();
        Map<String, String> refreshed =
                Map.of(
                        "accessToken", refreshedJson.getString("accessToken"),
                        "refreshToken", refreshedJson.getString("refreshToken"));

        // Normal rotation keeps token_version: both access tokens stay valid.
        assertProtectedStatus(login.get("accessToken"), HttpStatus.OK.value());
        assertProtectedStatus(refreshed.get("accessToken"), HttpStatus.OK.value());
        long versionBeforeReuse = currentTokenVersion(adminId);

        given().spec(SPEC)
                .body(Map.of("refreshToken", login.get("refreshToken")))
                .when()
                .post(AUTH_PATH + "/refresh")
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value());

        Assertions.assertEquals(versionBeforeReuse + 1, currentTokenVersion(adminId));
        assertProtectedStatus(login.get("accessToken"), HttpStatus.UNAUTHORIZED.value());
        assertProtectedStatus(refreshed.get("accessToken"), HttpStatus.UNAUTHORIZED.value());
    }

    @DisplayName("Revoked access token gets the standard 401 body")
    @Test
    void shouldReturnStandard401Body_whenAccessTokenRevoked() {
        String oldToken = login(MOBILE_API_KEY, "body-device").get("accessToken");
        login(MOBILE_API_KEY, "body-device");

        Response response =
                given().spec(SPEC)
                        .header("Authorization", "Bearer " + oldToken)
                        .when()
                        .get("/v1/colors")
                        .then()
                        .statusCode(HttpStatus.UNAUTHORIZED.value())
                        .extract()
                        .response();

        assertProblem(
                response,
                401,
                "Não Autenticado",
                "Autenticação necessária para acessar este recurso.",
                null,
                "authentication.required");
    }

    @DisplayName("ADMIN login without boundDeviceId succeeds, keeps DB unbound, and refreshes")
    @Test
    void shouldAllowAdminLoginWithoutBoundDeviceIdAndRefresh() {
        JsonPath loginJson =
                given().spec(SPEC)
                        .header(API_KEY_HEADER, MOBILE_API_KEY)
                        .body(Map.of("appVersion", "1.0.0"))
                        .when()
                        .post(AUTH_PATH + "/token")
                        .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract()
                        .jsonPath();

        String accessToken = loginJson.getString("accessToken");
        String refreshToken = loginJson.getString("refreshToken");

        Map<String, Object> dbRow =
                jdbcTemplate.queryForMap(
                        "SELECT bound_device_id, bound_at FROM api_keys WHERE id = " + adminId);
        Assertions.assertNull(dbRow.get("bound_device_id"));
        Assertions.assertNull(dbRow.get("bound_at"));

        given().spec(SPEC)
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .get("/v1/admin/api-keys")
                .then()
                .statusCode(HttpStatus.OK.value());

        String refreshedAccessToken =
                given().spec(SPEC)
                        .body(Map.of("refreshToken", refreshToken))
                        .when()
                        .post(AUTH_PATH + "/refresh")
                        .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract()
                        .jsonPath()
                        .getString("accessToken");

        given().spec(SPEC)
                .header("Authorization", "Bearer " + refreshedAccessToken)
                .when()
                .get("/v1/admin/api-keys")
                .then()
                .statusCode(HttpStatus.OK.value());
    }

    @DisplayName("ADMIN login across different devices succeeds with single-session enforcement")
    @Test
    void shouldAllowAdminLoginFromDifferentDevicesAndEnforceSingleSession() {
        Map<String, String> login1 = login(MOBILE_API_KEY, "device-a");
        Map<String, String> login2 = login(MOBILE_API_KEY, "device-b");

        assertProtectedStatus(login1.get("accessToken"), HttpStatus.UNAUTHORIZED.value());
        assertProtectedStatus(login2.get("accessToken"), HttpStatus.OK.value());

        String boundDeviceId =
                jdbcTemplate.queryForObject(
                        "SELECT bound_device_id FROM api_keys WHERE id = " + adminId, String.class);
        Assertions.assertNull(boundDeviceId);
    }

    @DisplayName("SERVICE login without boundDeviceId returns 400")
    @Test
    void shouldReturn400_whenServiceLoginWithoutBoundDeviceId() {
        given().spec(SPEC)
                .header(API_KEY_HEADER, SERVICE_API_KEY)
                .body(Map.of("appVersion", "1.0.0"))
                .when()
                .post(AUTH_PATH + "/token")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("status", equalTo(400))
                .body("code", equalTo("requisicao.invalida"));
    }

    private Map<String, String> login(String apiKey, String boundDeviceId) {
        JsonPath json =
                given().spec(SPEC)
                        .header(API_KEY_HEADER, apiKey)
                        .body(Map.of("boundDeviceId", boundDeviceId, "appVersion", "1.0.0"))
                        .when()
                        .post(AUTH_PATH + "/token")
                        .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract()
                        .jsonPath();
        return Map.of(
                "accessToken", json.getString("accessToken"),
                "refreshToken", json.getString("refreshToken"));
    }

    private void assertProtectedStatus(String accessToken, int expectedStatus) {
        given().spec(SPEC)
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .get("/v1/colors")
                .then()
                .statusCode(expectedStatus);
    }

    private long currentTokenVersion(long apiKeyId) {
        return jdbcTemplate.queryForObject(
                "SELECT token_version FROM api_keys WHERE id = ?", Long.class, apiKeyId);
    }

    private static void assertTokenResponse(Response response, boolean expectRefreshExpiry) {
        JsonPath json = response.jsonPath();
        Assertions.assertFalse(json.getString("accessToken").isBlank());
        Assertions.assertTrue(json.getString("refreshToken").startsWith("rt_"));
        Assertions.assertEquals("Bearer", json.getString("tokenType"));
        Assertions.assertEquals(900, json.getInt("expiresIn"));
        if (expectRefreshExpiry) {
            Assertions.assertEquals(604_800, json.getInt("refreshExpiresIn"));
        }
    }

    private static void assertProblem(
            Response response,
            int status,
            String title,
            String detail,
            String instance,
            String code) {
        JsonPath json = response.jsonPath();
        Assertions.assertEquals(status, json.getInt("status"));
        Assertions.assertEquals(title, json.getString("title"));
        Assertions.assertEquals(detail, json.getString("detail"));
        Assertions.assertEquals(code, json.getString("code"));
        if (instance != null) {
            Assertions.assertEquals(instance, json.getString("instance"));
        }
    }
}
