package com.tproject.workshop.service.auth;

import com.tproject.workshop.dto.auth.RefreshTokenRequest;
import com.tproject.workshop.dto.auth.RefreshTokenResponse;
import com.tproject.workshop.dto.auth.TokenRequest;
import com.tproject.workshop.dto.auth.TokenResponse;
import com.tproject.workshop.model.ApiKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

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
     * @param request      device/client data
     * @return access token + refresh token
     */
    public TokenResponse authenticate(String apiKeyHeader, TokenRequest request) {
        log.info("Token request for deviceId: {}", request.deviceId());

        // Validate and get the API Key entity
        apiKeyService.validateApiKey(apiKeyHeader);
        ApiKey apiKey = apiKeyService.findByKeyValue(apiKeyHeader);

        // Generate JWT access token with claims
        String accessToken = generateAccessToken(apiKey, request.deviceId(), request.appVersion());

        // Generate refresh token linked to the API Key
        String refreshToken = refreshTokenService.createRefreshToken(apiKey, request.deviceId());

        log.info("Token generated successfully for client: {}, user: {}", 
                apiKey.getClientName(), apiKey.getUserIdentifier());

        return new TokenResponse(
                accessToken,
                refreshToken,
                "Bearer",
                accessTokenExpiration,
                refreshTokenExpiration
        );
    }

    /**
     * Renews the access token using a valid refresh token.
     *
     * @param request contains the refresh token
     * @return new access token
     */
    public RefreshTokenResponse refreshToken(RefreshTokenRequest request) {
        log.debug("Refresh token request");

        // Validate refresh token and get associated API Key
        ApiKey apiKey = refreshTokenService.validateRefreshToken(request.refreshToken());

        // We don't have deviceId/appVersion easily available here without changing 
        // the refresh request or saving them in the refresh token. 
        // For now, we generate a new token using data from the API Key.
        String accessToken = generateAccessToken(apiKey, "unknown", "unknown");

        log.debug("Access token renewed for client: {}, user: {}", 
                apiKey.getClientName(), apiKey.getUserIdentifier());

        return new RefreshTokenResponse(
                accessToken,
                "Bearer",
                accessTokenExpiration
        );
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

    private String generateAccessToken(ApiKey apiKey, String deviceId, String appVersion) {
        Map<String, Object> claims = Map.of(
                "roles", List.of("ROLE_" + apiKey.getRole().name()),
                "platform", apiKey.getPlatform().name(),
                "userIdentifier", apiKey.getUserIdentifier(),
                "deviceId", deviceId,
                "appVersion", appVersion
        );

        return jwtService.generateAccessToken(apiKey.getClientName(), claims);
    }
}