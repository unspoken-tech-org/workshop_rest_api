package com.tproject.workshop.service.auth;

import com.tproject.workshop.exception.InvalidTokenException;
import com.tproject.workshop.model.ApiKey;
import com.tproject.workshop.model.RefreshToken;
import com.tproject.workshop.repository.RefreshTokenRepository;
import com.tproject.workshop.util.KeyGeneratorUtils;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenService {

    public record RotationResult(ApiKey apiKey, String refreshToken) {}

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.refresh-token.expiration}")
    private long refreshTokenExpiration;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * Generates a new refresh token, stores its SHA-256 hash in the database, and returns the
     * plaintext token once.
     */
    @Transactional
    public String createRefreshToken(ApiKey apiKey) {
        // Revoke previous tokens for the same API Key
        refreshTokenRepository.revokeByApiKeyId(apiKey.getId());

        return issueAndSaveRefreshToken(apiKey);
    }

    /**
     * Validates and rotates the refresh token (revoking the presented token and issuing a new one).
     * Detects reuse of already-revoked tokens and revokes all active refresh tokens for the
     * associated API key to contain theft.
     *
     * @param token the plaintext refresh token
     * @return the associated ApiKey and newly issued plaintext refresh token
     * @throws InvalidTokenException if token is not found, revoked, or expired, or if the
     *     associated API key is inactive or expired
     */
    @Transactional(noRollbackFor = InvalidTokenException.class)
    public RotationResult rotateRefreshToken(String token) {
        if (token == null || token.isBlank()) {
            throw new InvalidTokenException("Refresh token not found");
        }

        String hashedToken = KeyGeneratorUtils.sha256Hex(token);
        RefreshToken refreshToken =
                refreshTokenRepository
                        .findByToken(hashedToken)
                        .orElseThrow(() -> new InvalidTokenException("Refresh token not found"));

        ApiKey apiKey = refreshToken.getApiKey();

        if (refreshToken.isRevoked()) {
            log.warn(
                    "Refresh token reuse detected; revoking active tokens for apiKeyId={}",
                    apiKey.getId());
            refreshTokenRepository.revokeByApiKeyId(apiKey.getId());
            throw new InvalidTokenException("Refresh token revoked");
        }

        Instant now = Instant.now();
        if (!refreshToken.getExpiresAt().isAfter(now)) {
            log.warn("Refresh token expired");
            throw new InvalidTokenException("Refresh token expired");
        }

        if (!apiKey.isActive()) {
            log.warn(
                    "Refresh token invalid because associated API Key is inactive (apiKeyId={})",
                    apiKey.getId());
            throw new InvalidTokenException("Associated API Key is inactive");
        }

        if (apiKey.getExpiresAt() != null && !apiKey.getExpiresAt().isAfter(now)) {
            log.warn(
                    "Refresh token invalid because associated API Key is expired (apiKeyId={})",
                    apiKey.getId());
            throw new InvalidTokenException("Associated API Key is expired");
        }

        int revoked = refreshTokenRepository.revokeIfActive(refreshToken.getId());
        if (revoked == 0) {
            log.warn(
                    "Concurrent refresh token reuse detected; revoking active tokens for apiKeyId={}",
                    apiKey.getId());
            refreshTokenRepository.revokeByApiKeyId(apiKey.getId());
            throw new InvalidTokenException("Refresh token revoked");
        }

        String newRefreshToken = issueAndSaveRefreshToken(apiKey);
        return new RotationResult(apiKey, newRefreshToken);
    }

    /** Revokes a specific refresh token. */
    @Transactional
    public void revokeRefreshToken(String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        String hashedToken = KeyGeneratorUtils.sha256Hex(token);
        refreshTokenRepository
                .findByToken(hashedToken)
                .ifPresent(rt -> {
                    rt.setRevoked(true);
                    refreshTokenRepository.save(rt);
                    log.info("Refresh token revoked");
                });
    }

    /** Revokes all refresh tokens for a client. */
    @Transactional
    public void revokeAllTokensForClient(String clientName) {
        refreshTokenRepository.revokeAllByClientName(clientName);
        log.info("All refresh tokens revoked for client");
    }

    private String issueAndSaveRefreshToken(ApiKey apiKey) {
        String rawToken = generateSecureToken();
        Instant now = Instant.now();

        RefreshToken refreshToken =
                RefreshToken.builder()
                        .token(KeyGeneratorUtils.sha256Hex(rawToken))
                        .apiKey(apiKey)
                        .expiresAt(now.plusSeconds(refreshTokenExpiration))
                        .revoked(false)
                        .createdAt(now)
                        .build();

        refreshTokenRepository.save(refreshToken);

        log.info(
                "Refresh token created for platform: {}, role: {}",
                apiKey.getPlatform(),
                apiKey.getRole());
        return rawToken;
    }

    private String generateSecureToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return "rt_" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
