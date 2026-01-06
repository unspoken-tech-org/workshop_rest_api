package com.tproject.workshop.service.auth;

import com.tproject.workshop.exception.InvalidTokenException;
import com.tproject.workshop.model.RefreshToken;
import com.tproject.workshop.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.refresh-token.expiration}")
    private long refreshTokenExpiration;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * Generates a new refresh token and saves it to the database.
     */
    @Transactional
    public String createRefreshToken(String clientId, String deviceId) {
        // Revoke previous tokens for the same device
        refreshTokenRepository.revokeByClientIdAndDeviceId(clientId, deviceId);

        // Generate new token
        String token = generateSecureToken();

        RefreshToken refreshToken = RefreshToken.builder()
                .token(token)
                .clientId(clientId)
                .deviceId(deviceId)
                .expiresAt(Instant.now().plusSeconds(refreshTokenExpiration))
                .revoked(false)
                .createdAt(Instant.now())
                .build();

        refreshTokenRepository.save(refreshToken);

        log.info("Refresh token created for client: {}, device: {}", clientId, deviceId);
        return token;
    }

    /**
     * Validates the refresh token and returns the clientId.
     */
    @Transactional(readOnly = true)
    public String validateRefreshToken(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new InvalidTokenException("Refresh token not found"));

        if (refreshToken.isRevoked()) {
            log.warn("Attempt to use a revoked refresh token");
            throw new InvalidTokenException("Refresh token revoked");
        }

        if (refreshToken.getExpiresAt().isBefore(Instant.now())) {
            log.warn("Refresh token expired");
            throw new InvalidTokenException("Refresh token expired");
        }

        return refreshToken.getClientId();
    }

    /**
     * Revokes a specific refresh token.
     */
    @Transactional
    public void revokeRefreshToken(String token) {
        refreshTokenRepository.findByToken(token)
                .ifPresent(rt -> {
                    rt.setRevoked(true);
                    refreshTokenRepository.save(rt);
                    log.info("Refresh token revoked");
                });
    }

    /**
     * Revokes all refresh tokens for a client.
     */
    @Transactional
    public void revokeAllTokensForClient(String clientId) {
        refreshTokenRepository.revokeAllByClientId(clientId);
        log.info("All refresh tokens revoked for client: {}", clientId);
    }

    private String generateSecureToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return "rt_" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
