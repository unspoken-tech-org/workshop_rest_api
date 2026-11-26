package com.tproject.workshop.integration.security;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.tproject.workshop.config.security.RsaKeyProperties;
import com.tproject.workshop.integration.support.AbstractIntegrationTest;
import com.tproject.workshop.integration.support.TestAuthClient;
import com.tproject.workshop.service.auth.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.restassured.path.json.JsonPath;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.jdbc.Sql;

/** Security contract tests for JWT claims, signature integrity, expiration, and revocation (I7). */
@Sql({"/test-scripts/AuthSetup.sql", "/test-scripts/resetTablesSequence.sql"})
class JwtSecurityIT extends AbstractIntegrationTest {

    private static final String PROTECTED_ENDPOINT = "/v1/brand";

    @Autowired private JwtService jwtService;

    @Autowired private RsaKeyProperties rsaKeys;

    @Test
    void shouldIssueJwtWithExpectedClaimsAndRoles() {
        JsonPath adminAuth = authenticate(TestAuthClient.MOBILE_API_KEY);
        String adminAccess = adminAuth.getString("accessToken");
        String adminRefresh = adminAuth.getString("refreshToken");

        assertThat(adminAccess).isNotBlank();
        assertThat(adminRefresh).isNotBlank().isNotEqualTo(adminAccess);

        Claims adminClaims = jwtService.validateAndGetClaims(adminAccess);
        assertThat(adminClaims.getIssuer()).isEqualTo("workshop-api");
        assertThat(adminClaims.getAudience()).containsExactly("workshop-api");
        assertThat(adminClaims.getSubject()).isEqualTo("test_app");
        assertThat(adminClaims.getExpiration()).isAfter(adminClaims.getIssuedAt());
        assertThat(adminClaims.get("apiKeyId", Number.class).longValue()).isEqualTo(1L);
        assertThat(adminClaims.get("tv", Number.class).longValue()).isEqualTo(1L);
        assertThat(adminClaims.get("roles", List.class)).containsExactly("ROLE_ADMIN");

        JsonPath serviceAuth = authenticate(TestAuthClient.SERVICE_API_KEY);
        Claims serviceClaims =
                jwtService.validateAndGetClaims(serviceAuth.getString("accessToken"));
        assertThat(serviceClaims.get("roles", List.class)).containsExactly("ROLE_SERVICE");
    }

    @Test
    void shouldRejectTamperedPayload() {
        String validToken = authClient.getAccessToken(TestAuthClient.MOBILE_API_KEY);
        String[] parts = validToken.split("\\.");
        assertThat(parts).hasSize(3);

        String originalPayload =
                new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        String tamperedPayload = originalPayload.replace("test_app", "tampered_app");
        String encodedTampered =
                Base64.getUrlEncoder()
                        .withoutPadding()
                        .encodeToString(tamperedPayload.getBytes(StandardCharsets.UTF_8));
        String tamperedToken = parts[0] + "." + encodedTampered + "." + parts[2];

        assertUnauthorized(tamperedToken);
    }

    @Test
    void shouldRejectWrongIssuerOrAudience() {
        String wrongIssuer =
                signToken("wrong-issuer", "workshop-api", Instant.now().plusSeconds(300));
        String wrongAudience =
                signToken("workshop-api", "wrong-audience", Instant.now().plusSeconds(300));

        assertUnauthorized(wrongIssuer);
        assertUnauthorized(wrongAudience);
    }

    @Test
    void shouldRejectExpiredToken() {
        String expiredToken =
                signToken("workshop-api", "workshop-api", Instant.now().minusSeconds(60));

        assertUnauthorized(expiredToken);
    }

    @Test
    void shouldRejectAccessTokenAfterLogoutOrKeyDeactivation() {
        JsonPath auth = authenticate(TestAuthClient.MOBILE_API_KEY);
        String accessToken = auth.getString("accessToken");
        String refreshToken = auth.getString("refreshToken");

        given().spec(SPEC)
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .get(PROTECTED_ENDPOINT)
                .then()
                .statusCode(HttpStatus.OK.value());

        given().spec(SPEC)
                .body(Map.of("refreshToken", refreshToken))
                .when()
                .post("/api/auth/revoke")
                .then()
                .statusCode(HttpStatus.NO_CONTENT.value());

        assertUnauthorized(accessToken);

        String freshToken = authenticate(TestAuthClient.MOBILE_API_KEY).getString("accessToken");
        jdbcTemplate.update("UPDATE api_keys SET active = false WHERE id = 1");
        assertUnauthorized(freshToken);
    }

    private JsonPath authenticate(String apiKey) {
        return given().spec(SPEC)
                .header("X-API-Key", apiKey)
                .body(Map.of("boundDeviceId", "jwt-security-it-device", "appVersion", "1.0.0-test"))
                .when()
                .post("/api/auth/token")
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .jsonPath();
    }

    private void assertUnauthorized(String token) {
        given().spec(SPEC)
                .header("Authorization", "Bearer " + token)
                .when()
                .get(PROTECTED_ENDPOINT)
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value())
                .body("code", equalTo("authentication.required"));
    }

    private String signToken(String issuer, String audience, Instant expiration) {
        Instant now = Instant.now();
        return Jwts.builder()
                .header()
                .type("JWT")
                .and()
                .subject("admin_user")
                .issuer(issuer)
                .audience()
                .add(audience)
                .and()
                .issuedAt(Date.from(now.minusSeconds(120)))
                .expiration(Date.from(expiration))
                .id(UUID.randomUUID().toString())
                .claim("apiKeyId", 1L)
                .claim("roles", List.of("ROLE_ADMIN"))
                .claim("tv", 0L)
                .signWith(rsaKeys.privateKey(), Jwts.SIG.RS256)
                .compact();
    }
}
