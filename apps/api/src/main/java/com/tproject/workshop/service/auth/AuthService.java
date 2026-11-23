package com.tproject.workshop.service.auth;

import com.tproject.workshop.dto.auth.RefreshTokenRequest;
import com.tproject.workshop.dto.auth.RefreshTokenResponse;
import com.tproject.workshop.dto.auth.TokenRequest;
import com.tproject.workshop.dto.auth.TokenResponse;
import com.tproject.workshop.model.ApiKey;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final ApiKeyService apiKeyService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    @Value("${jwt.access-token.expiration}")
    private long accessTokenExpiration;

    @Value("${jwt.refresh-token.expiration}")
    private long refreshTokenExpiration;

    /**
     * Authenticates using an API Key and returns JWT tokens.
     *
     * @param apiKeyHeader the API Key from header
     * @param request device/client data
     * @return access token + refresh token
     */
    public TokenResponse authenticate(String apiKeyHeader, TokenRequest request) {
        // boundDeviceId is supplied in the request body and must not enter
        // application logs.
        log.info("Token request received");

        // Validate and get the API Key entity
        apiKeyService.validateApiKey(apiKeyHeader);
        ApiKey apiKey = apiKeyService.findByKeyValue(apiKeyHeader);

        apiKeyService.bindDeviceOrThrow(apiKey, request.boundDeviceId());

        // A new login invalidates access tokens from previous sessions. apiKey is detached, so
        // the version written by this login comes from the UPDATE ... RETURNING itself.
        long tokenVersion = apiKeyService.incrementTokenVersion(apiKey.getId());

        // Generate JWT access token with claims
        String accessToken =
                generateAccessToken(
                        apiKey, tokenVersion, request.boundDeviceId(), request.appVersion());

        // Generate refresh token linked to the API Key
        String refreshToken = refreshTokenService.createRefreshToken(apiKey);

        log.info(
                "Token generated successfully for platform: {}, role: {}",
                apiKey.getPlatform(),
                apiKey.getRole());

        return new TokenResponse(
                accessToken, refreshToken, "Bearer", accessTokenExpiration, refreshTokenExpiration);
    }

    /**
     * Renews the access token using a valid refresh token.
     *
     * @param request contains the refresh token
     * @return new access token
     */
    public RefreshTokenResponse refreshToken(RefreshTokenRequest request) {
        log.debug("Refresh token request");

        // Validate, rotate refresh token, and get associated API Key
        RefreshTokenService.RotationResult rotation =
                refreshTokenService.rotateRefreshToken(request.refreshToken());
        ApiKey apiKey = rotation.apiKey();

        // We don't have appVersion easily available here without changing
        // the refresh request or saving them in the refresh token.
        String boundDeviceId = Optional.ofNullable(apiKey.getBoundDeviceId()).orElse("unknown");
        // A normal rotation keeps token_version so in-flight requests are not dropped.
        String accessToken =
                generateAccessToken(apiKey, apiKey.getTokenVersion(), boundDeviceId, "unknown");

        log.debug(
                "Access token renewed successfully for platform: {}, role: {}",
                apiKey.getPlatform(),
                apiKey.getRole());

        return new RefreshTokenResponse(
                accessToken, rotation.refreshToken(), "Bearer", accessTokenExpiration);
    }

    /**
     * Revokes a refresh token (logout).
     *
     * @param request contains the refresh token to be revoked
     */
    public void revokeToken(RefreshTokenRequest request) {
        log.info("Refresh token revocation request");
        refreshTokenService.revokeRefreshToken(request.refreshToken());
        log.info("Refresh token revoked successfully");
    }

    private String generateAccessToken(
            ApiKey apiKey, long tokenVersion, String boundDeviceId, String appVersion) {
        Map<String, Object> claims =
                Map.of(
                        "apiKeyId", apiKey.getId(),
                        "roles", List.of("ROLE_" + apiKey.getRole().name()),
                        "platform", apiKey.getPlatform().name(),
                        "userIdentifier", apiKey.getUserIdentifier(),
                        "boundDeviceId", boundDeviceId,
                        "appVersion", appVersion,
                        "tv", tokenVersion);

        return jwtService.generateAccessToken(apiKey.getClientName(), claims);
    }
}
