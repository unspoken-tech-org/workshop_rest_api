package com.tproject.workshop.service.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tproject.workshop.config.security.RsaKeyProperties;
import com.tproject.workshop.exception.InvalidTokenException;
import com.tproject.workshop.exception.TokenExpiredException;
import com.tproject.workshop.integration.support.TestClock;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Clock;
import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class JwtServiceTest {

    private static final long ACCESS_TOKEN_SECONDS = 900;

    private static KeyPair keyPair;
    private static KeyPair otherKeyPair;

    private JwtService jwtService;

    @BeforeAll
    static void generateKeys() throws NoSuchAlgorithmException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        keyPair = generator.generateKeyPair();
        otherKeyPair = generator.generateKeyPair();
    }

    @BeforeEach
    void setUp() {
        jwtService = serviceAt(TestClock.FIXED);
    }

    private static JwtService serviceAt(Clock clock) {
        JwtService service =
                new JwtService(
                        new RsaKeyProperties(
                                (RSAPublicKey) keyPair.getPublic(),
                                (RSAPrivateKey) keyPair.getPrivate()),
                        clock);
        ReflectionTestUtils.setField(service, "accessTokenExpiration", ACCESS_TOKEN_SECONDS);
        return service;
    }

    @Test
    @DisplayName("A generated token carries issuer, audience, subject, custom claims and expiry")
    void shouldGenerateToken_whenClaimsAreGiven() {
        String token =
                jwtService.generateAccessToken(
                        "test_app", Map.of("apiKeyId", 7, "roles", List.of("ROLE_ADMIN")));

        Claims claims = jwtService.validateAndGetClaims(token);

        assertThat(claims.getSubject()).isEqualTo("test_app");
        assertThat(claims.getIssuer()).isEqualTo("workshop-api");
        assertThat(claims.getAudience()).containsExactly("workshop-api");
        assertThat(claims.get("apiKeyId", Integer.class)).isEqualTo(7);
        assertThat(claims.get("roles", List.class)).containsExactly("ROLE_ADMIN");
        assertThat(claims.getId()).isNotBlank();
        assertThat(claims.getIssuedAt().toInstant()).isEqualTo(TestClock.DEFAULT_INSTANT);
        assertThat(claims.getExpiration().toInstant())
                .isEqualTo(TestClock.DEFAULT_INSTANT.plusSeconds(ACCESS_TOKEN_SECONDS));
    }

    @Test
    @DisplayName("Each token gets a distinct jti")
    void shouldGenerateDistinctIds_whenGeneratingTwoTokens() {
        String first = jwtService.generateAccessToken("test_app", Map.of());
        String second = jwtService.generateAccessToken("test_app", Map.of());

        assertThat(jwtService.validateAndGetClaims(first).getId())
                .isNotEqualTo(jwtService.validateAndGetClaims(second).getId());
    }

    @Test
    @DisplayName("getSubject returns the token subject")
    void shouldReturnSubject_whenTokenIsValid() {
        String token = jwtService.generateAccessToken("test_app", Map.of());

        assertThat(jwtService.getSubject(token)).isEqualTo("test_app");
    }

    @Test
    @DisplayName("A token past its expiry is rejected as expired")
    void shouldThrowTokenExpired_whenTokenIsPastExpiry() {
        String token = jwtService.generateAccessToken("test_app", Map.of());
        JwtService later =
                serviceAt(
                        TestClock.fixed(
                                TestClock.DEFAULT_INSTANT.plus(
                                        Duration.ofSeconds(ACCESS_TOKEN_SECONDS + 1))));

        assertThatThrownBy(() -> later.validateAndGetClaims(token))
                .isInstanceOf(TokenExpiredException.class);
        assertThat(later.isTokenExpired(token)).isTrue();
    }

    @Test
    @DisplayName("A token is still valid right before its expiry")
    void shouldAcceptToken_whenJustBeforeExpiry() {
        String token = jwtService.generateAccessToken("test_app", Map.of());
        JwtService almostExpired =
                serviceAt(
                        TestClock.fixed(
                                TestClock.DEFAULT_INSTANT.plus(
                                        Duration.ofSeconds(ACCESS_TOKEN_SECONDS - 1))));

        assertThat(almostExpired.validateAndGetClaims(token).getSubject()).isEqualTo("test_app");
        assertThat(almostExpired.isTokenExpired(token)).isFalse();
    }

    @Test
    @DisplayName("A token signed by another key is rejected")
    void shouldThrowInvalidToken_whenSignatureDoesNotMatch() {
        String forged = signedWith(otherKeyPair, "workshop-api", "workshop-api");

        assertThatThrownBy(() -> jwtService.validateAndGetClaims(forged))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessage("Invalid token signature");
        assertThat(jwtService.isTokenExpired(forged)).isTrue();
    }

    @Test
    @DisplayName("A token with a different issuer is rejected")
    void shouldThrowInvalidToken_whenIssuerIsWrong() {
        String token = signedWith(keyPair, "someone-else", "workshop-api");

        assertThatThrownBy(() -> jwtService.validateAndGetClaims(token))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    @DisplayName("A token with a different audience is rejected")
    void shouldThrowInvalidToken_whenAudienceIsWrong() {
        String token = signedWith(keyPair, "workshop-api", "another-api");

        assertThatThrownBy(() -> jwtService.validateAndGetClaims(token))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    @DisplayName("A malformed token is rejected")
    void shouldThrowInvalidToken_whenTokenIsMalformed() {
        assertThatThrownBy(() -> jwtService.validateAndGetClaims("not.a.jwt"))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessage("Invalid token");
        assertThat(jwtService.isTokenExpired("not.a.jwt")).isTrue();
    }

    private static String signedWith(KeyPair pair, String issuer, String audience) {
        return Jwts.builder()
                .subject("test_app")
                .issuer(issuer)
                .audience()
                .add(audience)
                .and()
                .issuedAt(Date.from(TestClock.DEFAULT_INSTANT))
                .expiration(Date.from(TestClock.DEFAULT_INSTANT.plusSeconds(ACCESS_TOKEN_SECONDS)))
                .signWith(pair.getPrivate(), Jwts.SIG.RS256)
                .compact();
    }
}
