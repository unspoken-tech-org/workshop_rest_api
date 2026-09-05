package com.tproject.workshop.service.auth;

import com.tproject.workshop.config.security.RsaKeyProperties;
import com.tproject.workshop.exception.InvalidTokenException;
import com.tproject.workshop.exception.TokenExpiredException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.SignatureException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class JwtService {

    private final RsaKeyProperties rsaKeys;

    @Value("${jwt.access-token.expiration}")
    private long accessTokenExpiration;

    private static final String ISSUER = "workshop-api";
    private static final String AUDIENCE = "workshop-api";

    /**
     * Generates a JWT Access Token signed with RS256.
     */
    public String generateAccessToken(String subject, Map<String, Object> claims) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(accessTokenExpiration);

        return Jwts.builder()
                .header()
                .type("JWT")
                .and()
                .subject(subject)
                .issuer(ISSUER)
                .audience().add(AUDIENCE).and()
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .id(UUID.randomUUID().toString())
                .claims(claims)
                .signWith(rsaKeys.privateKey(), Jwts.SIG.RS256)
                .compact();
    }

    /**
     * Validates the token and extracts claims.
     * Throws exception if invalid or expired.
     *
     * @return Claims object
     * @throws InvalidTokenException if token is invalid
     * @throws TokenExpiredException if token is expired
     */
    public Claims validateAndGetClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(rsaKeys.publicKey())
                    .requireIssuer(ISSUER)
                    .requireAudience(AUDIENCE)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            log.warn("Token expired: {}", e.getMessage());
            throw new TokenExpiredException("Token expired");
        } catch (SignatureException e) {
            log.warn("Invalid signature: {}", e.getMessage());
            throw new InvalidTokenException("Invalid token signature");
        } catch (JwtException e) {
            log.warn("Invalid token: {}", e.getMessage());
            throw new InvalidTokenException("Invalid token: " + e.getMessage());
        }
    }

    /**
     * Extracts the subject (client identifier) from the token.
     */
    public String getSubject(String token) {
        return validateAndGetClaims(token).getSubject();
    }

    /**
     * Checks if the token is expired (does not throw exception).
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = validateAndGetClaims(token);
            return claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return true;
        }
    }
}
