package com.tproject.workshop.integration.controller;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.startsWith;

import com.tproject.workshop.config.security.RsaKeyProperties;
import com.tproject.workshop.integration.TestAuthHelper;
import com.tproject.workshop.integration.support.AbstractIntegrationTest;
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
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Sql({
    "/test-scripts/cleanTestData.sql",
    "/test-scripts/AuthSetup.sql",
    "/test-scripts/resetTablesSequence.sql"
})
public class AuthControllerIT extends AbstractIntegrationTest {

    private static final String AUTH_PATH = "/api/auth";
    private static final String API_KEY_HEADER = "X-API-Key";

    @Autowired private RsaKeyProperties rsaKeyProperties;

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
                        .statusCode(HttpStatus.SC_OK)
                        .extract()
                        .response();

        super.validateResponseIgnoreAttributes(
                index, response, List.of("accessToken", "refreshToken"));
    }

    private static Stream<Arguments> validApiKeyArguments() {
        return Stream.of(
                Arguments.of(1, "MOBILE", TestAuthHelper.MOBILE_API_KEY),
                Arguments.of(2, "WEB", TestAuthHelper.WEB_API_KEY),
                Arguments.of(3, "DESKTOP", TestAuthHelper.DESKTOP_API_KEY),
                Arguments.of(4, "SERVER", TestAuthHelper.SERVER_API_KEY));
    }

    @Order(2)
    @DisplayName("Reject token with invalid API Key")
    @Test
    public void shouldReturn401_whenInvalidApiKey() {
        Response response =
                given().spec(SPEC)
                        .header(API_KEY_HEADER, TestAuthHelper.INVALID_API_KEY)
                        .body(
                                Map.of(
                                        "boundDeviceId", "test-device",
                                        "appVersion", "1.0.0"))
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
                .body(
                        Map.of(
                                "boundDeviceId", "test-device",
                                "appVersion", "1.0.0"))
                .when()
                .post(AUTH_PATH + "/token")
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST);
    }

    @Order(4)
    @DisplayName("Reject token with expired API Key")
    @Test
    public void shouldReturn401_whenExpiredApiKey() {
        Response response =
                given().spec(SPEC)
                        .header(API_KEY_HEADER, TestAuthHelper.EXPIRED_API_KEY)
                        .body(
                                Map.of(
                                        "boundDeviceId", "test-device",
                                        "appVersion", "1.0.0"))
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
        Response response =
                given().spec(SPEC)
                        .header(API_KEY_HEADER, TestAuthHelper.INACTIVE_API_KEY)
                        .body(
                                Map.of(
                                        "boundDeviceId", "test-device",
                                        "appVersion", "1.0.0"))
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

        Response response =
                given().spec(SPEC)
                        .body(Map.of("refreshToken", refreshToken))
                        .when()
                        .post(AUTH_PATH + "/refresh")
                        .then()
                        .statusCode(HttpStatus.SC_OK)
                        .extract()
                        .response();

        super.validateResponseIgnoreAttributes(
                "refresh_success", response, List.of("accessToken", "refreshToken"));
    }

    @Order(7)
    @DisplayName("Reject refresh with invalid refresh token")
    @Test
    public void shouldReturn401_whenInvalidRefreshToken() {
        Response response =
                given().spec(SPEC)
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
        // The token 'rt_EXPIRED_TOKEN_1234567890' is inserted via AuthSetup.sql with an expired
        // date
        Response response =
                given().spec(SPEC)
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

    @Order(10)
    @DisplayName("Refresh tokens are scoped to API Key")
    @Test
    public void shouldAllowRefreshTokensForDifferentUsersOnSameDevice() {
        String boundDeviceId = "shared-device-id";

        String firstUserRefreshToken =
                given().spec(SPEC)
                        .header(API_KEY_HEADER, TestAuthHelper.MOBILE_API_KEY)
                        .body(Map.of("boundDeviceId", boundDeviceId, "appVersion", "1.0.0"))
                        .when()
                        .post(AUTH_PATH + "/token")
                        .then()
                        .statusCode(HttpStatus.SC_OK)
                        .extract()
                        .jsonPath()
                        .getString("refreshToken");

        given().spec(SPEC)
                .header(API_KEY_HEADER, TestAuthHelper.MOBILE_API_KEY_SECOND_USER)
                .body(Map.of("boundDeviceId", boundDeviceId, "appVersion", "1.0.0"))
                .when()
                .post(AUTH_PATH + "/token")
                .then()
                .statusCode(HttpStatus.SC_OK);

        given().spec(SPEC)
                .body(Map.of("refreshToken", firstUserRefreshToken))
                .when()
                .post(AUTH_PATH + "/refresh")
                .then()
                .statusCode(HttpStatus.SC_OK);
    }

    // ========== Protected Endpoint Tests ==========

    @Order(11)
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

    @Order(12)
    @DisplayName("Reject access to protected endpoint without JWT")
    @Test
    public void shouldReturn401_whenAccessingProtectedEndpointWithoutJwt() {
        given().spec(SPEC)
                .when()
                .get("/v1/admin/api-keys")
                .then()
                .statusCode(HttpStatus.SC_UNAUTHORIZED);
    }

    @Order(13)
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

    @Order(14)
    @DisplayName("Reject access with JWT signed by a different RSA key (Key Rotation Scenario)")
    @Test
    public void shouldReturn401_whenJwtSignedByDifferentKey() throws NoSuchAlgorithmException {
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
                .statusCode(HttpStatus.SC_UNAUTHORIZED);
    }

    @Order(15)
    @DisplayName("Reject token when API Key is bound to another device")
    @Test
    public void shouldReturn409_whenApiKeyBoundToAnotherDevice() {
        given().spec(SPEC)
                .header(API_KEY_HEADER, TestAuthHelper.SERVICE_API_KEY)
                .body(
                        Map.of(
                                "boundDeviceId", "device-a",
                                "appVersion", "1.0.0"))
                .when()
                .post(AUTH_PATH + "/token")
                .then()
                .statusCode(HttpStatus.SC_OK);

        given().spec(SPEC)
                .header(API_KEY_HEADER, TestAuthHelper.SERVICE_API_KEY)
                .body(
                        Map.of(
                                "boundDeviceId", "device-b",
                                "appVersion", "1.0.0"))
                .when()
                .post(AUTH_PATH + "/token")
                .then()
                .statusCode(HttpStatus.SC_CONFLICT);
    }

    // ========== Security Hardening Tests (2.4) ==========

    @Order(16)
    @DisplayName("Rotate refresh token and revoke family on reuse")
    @Test
    public void shouldRotateRefreshTokenAndRevokeAllOnReuse() {
        String initialRefreshToken =
                TestAuthHelper.getRefreshToken(SPEC, TestAuthHelper.MOBILE_API_KEY);

        String rotatedRefreshToken =
                given().spec(SPEC)
                        .body(Map.of("refreshToken", initialRefreshToken))
                        .when()
                        .post(AUTH_PATH + "/refresh")
                        .then()
                        .statusCode(HttpStatus.SC_OK)
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
                .statusCode(HttpStatus.SC_UNAUTHORIZED);

        given().spec(SPEC)
                .body(Map.of("refreshToken", rotatedRefreshToken))
                .when()
                .post(AUTH_PATH + "/refresh")
                .then()
                .statusCode(HttpStatus.SC_UNAUTHORIZED);
    }

    @Order(17)
    @DisplayName("Reject refresh when backing API Key is expired")
    @Test
    public void shouldReturn401_whenRefreshingWithExpiredApiKey() {
        String refreshToken = TestAuthHelper.getRefreshToken(SPEC, TestAuthHelper.SERVICE_API_KEY);

        jdbcTemplate.update(
                "UPDATE api_keys SET expires_at = CURRENT_TIMESTAMP - INTERVAL '1 minute' WHERE id = 7");

        given().spec(SPEC)
                .body(Map.of("refreshToken", refreshToken))
                .when()
                .post(AUTH_PATH + "/refresh")
                .then()
                .statusCode(HttpStatus.SC_UNAUTHORIZED);
    }

    @Order(18)
    @DisplayName(
            "Immediately reject access token when API Key is revoked, expired, or device-reset")
    @Test
    public void shouldReturn401Immediately_whenApiKeyRevokedOrExpiredOrReset() {
        String serviceToken = TestAuthHelper.getAccessToken(SPEC, TestAuthHelper.SERVICE_API_KEY);

        given().spec(SPEC)
                .header("Authorization", "Bearer " + serviceToken)
                .when()
                .get("/v1/colors")
                .then()
                .statusCode(HttpStatus.SC_OK);

        // Revoke API Key 7 (SERVICE) -> access token must immediately fail with 401
        given().spec(getAuthenticatedSpec())
                .when()
                .delete("/v1/admin/api-keys/7")
                .then()
                .statusCode(HttpStatus.SC_NO_CONTENT);

        given().spec(SPEC)
                .header("Authorization", "Bearer " + serviceToken)
                .when()
                .get("/v1/colors")
                .then()
                .statusCode(HttpStatus.SC_UNAUTHORIZED);

        // Device reset on API Key 8 -> access token must immediately fail with 401
        String secondAdminToken =
                TestAuthHelper.getAccessToken(SPEC, TestAuthHelper.MOBILE_API_KEY_SECOND_USER);
        given().spec(getAuthenticatedSpec())
                .when()
                .post("/v1/admin/api-keys/8/reset-device")
                .then()
                .statusCode(HttpStatus.SC_NO_CONTENT);

        given().spec(SPEC)
                .header("Authorization", "Bearer " + secondAdminToken)
                .when()
                .get("/v1/colors")
                .then()
                .statusCode(HttpStatus.SC_UNAUTHORIZED);

        // Expire API Key 1 -> access token must immediately fail with 401
        String adminToken = TestAuthHelper.getAccessToken(SPEC, TestAuthHelper.MOBILE_API_KEY);
        jdbcTemplate.update(
                "UPDATE api_keys SET expires_at = CURRENT_TIMESTAMP - INTERVAL '1 minute' WHERE id = 1");

        given().spec(SPEC)
                .header("Authorization", "Bearer " + adminToken)
                .when()
                .get("/v1/colors")
                .then()
                .statusCode(HttpStatus.SC_UNAUTHORIZED);
    }

    @Order(19)
    @DisplayName("Enforce role-based authorization across business and admin endpoints")
    @Test
    public void shouldEnforceRoleAuthorizationAcrossControllers() {
        // SERVICE role can access business endpoints used by Flutter
        given().spec(getServiceAuthenticatedSpec())
                .when()
                .get("/v1/colors")
                .then()
                .statusCode(HttpStatus.SC_OK);

        // SERVICE role cannot access admin endpoints
        given().spec(getServiceAuthenticatedSpec())
                .when()
                .get("/v1/admin/api-keys")
                .then()
                .statusCode(HttpStatus.SC_FORBIDDEN);

        // A valid JWT bound to an active key but carrying an unauthorized role gets 403 on business
        // endpoints
        jdbcTemplate.update("UPDATE api_keys SET bound_device_id = ? WHERE id = 7", "guest-device");
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
                        .claim("apiKeyId", 7L)
                        .claim("boundDeviceId", "guest-device")
                        .claim("tv", currentTokenVersion(7L))
                        .claim("roles", List.of("ROLE_GUEST"))
                        .signWith(rsaKeyProperties.privateKey(), Jwts.SIG.RS256)
                        .compact();

        given().spec(SPEC)
                .header("Authorization", "Bearer " + guestToken)
                .when()
                .get("/v1/colors")
                .then()
                .statusCode(HttpStatus.SC_FORBIDDEN);
    }

    @Order(20)
    @DisplayName("Store SHA-256 hashes in DB instead of plaintext API keys and refresh tokens")
    @Test
    public void shouldStoreSha256HashesInDatabase() {
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
                        .statusCode(HttpStatus.SC_CREATED)
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
                        .statusCode(HttpStatus.SC_OK)
                        .extract()
                        .jsonPath()
                        .getString("maskedKeyValue");
        Assertions.assertEquals("****", maskedKey);

        String plaintextRefresh = TestAuthHelper.getRefreshToken(SPEC, plaintextKey);
        String storedRefreshHash =
                jdbcTemplate.queryForObject(
                        "SELECT token FROM refresh_tokens WHERE api_key_id = ? AND revoked = false",
                        String.class,
                        createdId);
        Assertions.assertEquals(KeyGeneratorUtils.sha256Hex(plaintextRefresh), storedRefreshHash);
        Assertions.assertNotEquals(plaintextRefresh, storedRefreshHash);
    }

    // ========== Input Size Limits ==========

    @Order(21)
    @DisplayName("Reject oversized auth fields with 400")
    @MethodSource("oversizedAuthFieldArguments")
    @ParameterizedTest(name = "{displayName} : {0} {1}")
    public void shouldReturn400_whenAuthFieldExceedsMaxSize(
            String path, String field, Map<String, Object> body) {
        given().spec(SPEC)
                .header(API_KEY_HEADER, TestAuthHelper.MOBILE_API_KEY)
                .body(body)
                .when()
                .post(AUTH_PATH + path)
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
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

    @Order(22)
    @DisplayName("Logout immediately revokes the access token; a new login works")
    @Test
    public void shouldReturn401ForAccessToken_afterLogout() {
        Map<String, String> login1 = login(TestAuthHelper.MOBILE_API_KEY, "revoke-device");
        assertProtectedStatus(login1.get("accessToken"), HttpStatus.SC_OK);

        given().spec(SPEC)
                .body(Map.of("refreshToken", login1.get("refreshToken")))
                .when()
                .post(AUTH_PATH + "/revoke")
                .then()
                .statusCode(HttpStatus.SC_NO_CONTENT);

        assertProtectedStatus(login1.get("accessToken"), HttpStatus.SC_UNAUTHORIZED);

        Map<String, String> login2 = login(TestAuthHelper.MOBILE_API_KEY, "revoke-device");
        assertProtectedStatus(login2.get("accessToken"), HttpStatus.SC_OK);
        assertProtectedStatus(login1.get("accessToken"), HttpStatus.SC_UNAUTHORIZED);
        Assertions.assertEquals(3L, currentTokenVersion(1L));
    }

    @Order(23)
    @DisplayName("A new login revokes access tokens from the previous login")
    @Test
    public void shouldReturn401ForOldAccessToken_afterNewLogin() {
        Map<String, String> login1 = login(TestAuthHelper.MOBILE_API_KEY, "relogin-device");
        Map<String, String> login2 = login(TestAuthHelper.MOBILE_API_KEY, "relogin-device");

        assertProtectedStatus(login1.get("accessToken"), HttpStatus.SC_UNAUTHORIZED);
        assertProtectedStatus(login2.get("accessToken"), HttpStatus.SC_OK);
    }

    @Order(24)
    @DisplayName("Refresh token reuse revokes access tokens of the API Key")
    @Test
    public void shouldReturn401ForAccessTokens_afterRefreshTokenReuse() {
        Map<String, String> login = login(TestAuthHelper.MOBILE_API_KEY, "reuse-device");

        JsonPath refreshedJson =
                given().spec(SPEC)
                        .body(Map.of("refreshToken", login.get("refreshToken")))
                        .when()
                        .post(AUTH_PATH + "/refresh")
                        .then()
                        .statusCode(HttpStatus.SC_OK)
                        .extract()
                        .jsonPath();
        Map<String, String> refreshed =
                Map.of(
                        "accessToken", refreshedJson.getString("accessToken"),
                        "refreshToken", refreshedJson.getString("refreshToken"));

        // Normal rotation keeps token_version: both access tokens stay valid.
        assertProtectedStatus(login.get("accessToken"), HttpStatus.SC_OK);
        assertProtectedStatus(refreshed.get("accessToken"), HttpStatus.SC_OK);
        long versionBeforeReuse = currentTokenVersion(1L);

        given().spec(SPEC)
                .body(Map.of("refreshToken", login.get("refreshToken")))
                .when()
                .post(AUTH_PATH + "/refresh")
                .then()
                .statusCode(HttpStatus.SC_UNAUTHORIZED);

        Assertions.assertEquals(versionBeforeReuse + 1, currentTokenVersion(1L));
        assertProtectedStatus(login.get("accessToken"), HttpStatus.SC_UNAUTHORIZED);
        assertProtectedStatus(refreshed.get("accessToken"), HttpStatus.SC_UNAUTHORIZED);
    }

    @Order(25)
    @DisplayName("Revoked access token gets the standard 401 body")
    @Test
    public void shouldReturnStandard401Body_whenAccessTokenRevoked() {
        String oldToken = login(TestAuthHelper.MOBILE_API_KEY, "body-device").get("accessToken");
        login(TestAuthHelper.MOBILE_API_KEY, "body-device");

        Response response =
                given().spec(SPEC)
                        .header("Authorization", "Bearer " + oldToken)
                        .when()
                        .get("/v1/colors")
                        .then()
                        .statusCode(HttpStatus.SC_UNAUTHORIZED)
                        .extract()
                        .response();

        super.validateResponse("revoked_access_token", response);
    }

    @Order(26)
    @DisplayName("ADMIN login without boundDeviceId succeeds, keeps DB unbound, and refreshes")
    @Test
    public void shouldAllowAdminLoginWithoutBoundDeviceIdAndRefresh() {
        JsonPath loginJson =
                given().spec(SPEC)
                        .header(API_KEY_HEADER, TestAuthHelper.MOBILE_API_KEY)
                        .body(Map.of("appVersion", "1.0.0"))
                        .when()
                        .post(AUTH_PATH + "/token")
                        .then()
                        .statusCode(HttpStatus.SC_OK)
                        .extract()
                        .jsonPath();

        String accessToken = loginJson.getString("accessToken");
        String refreshToken = loginJson.getString("refreshToken");

        Map<String, Object> dbRow =
                jdbcTemplate.queryForMap(
                        "SELECT bound_device_id, bound_at FROM api_keys WHERE id = 1");
        Assertions.assertNull(dbRow.get("bound_device_id"));
        Assertions.assertNull(dbRow.get("bound_at"));

        given().spec(SPEC)
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .get("/v1/admin/api-keys")
                .then()
                .statusCode(HttpStatus.SC_OK);

        String refreshedAccessToken =
                given().spec(SPEC)
                        .body(Map.of("refreshToken", refreshToken))
                        .when()
                        .post(AUTH_PATH + "/refresh")
                        .then()
                        .statusCode(HttpStatus.SC_OK)
                        .extract()
                        .jsonPath()
                        .getString("accessToken");

        given().spec(SPEC)
                .header("Authorization", "Bearer " + refreshedAccessToken)
                .when()
                .get("/v1/admin/api-keys")
                .then()
                .statusCode(HttpStatus.SC_OK);
    }

    @Order(27)
    @DisplayName("ADMIN login across different devices succeeds with single-session enforcement")
    @Test
    public void shouldAllowAdminLoginFromDifferentDevicesAndEnforceSingleSession() {
        Map<String, String> login1 = login(TestAuthHelper.MOBILE_API_KEY, "device-a");
        Map<String, String> login2 = login(TestAuthHelper.MOBILE_API_KEY, "device-b");

        assertProtectedStatus(login1.get("accessToken"), HttpStatus.SC_UNAUTHORIZED);
        assertProtectedStatus(login2.get("accessToken"), HttpStatus.SC_OK);

        String boundDeviceId =
                jdbcTemplate.queryForObject(
                        "SELECT bound_device_id FROM api_keys WHERE id = 1", String.class);
        Assertions.assertNull(boundDeviceId);
    }

    @Order(28)
    @DisplayName("SERVICE login without boundDeviceId returns 400")
    @Test
    public void shouldReturn400_whenServiceLoginWithoutBoundDeviceId() {
        given().spec(SPEC)
                .header(API_KEY_HEADER, TestAuthHelper.SERVICE_API_KEY)
                .body(Map.of("appVersion", "1.0.0"))
                .when()
                .post(AUTH_PATH + "/token")
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
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
                        .statusCode(HttpStatus.SC_OK)
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
}
