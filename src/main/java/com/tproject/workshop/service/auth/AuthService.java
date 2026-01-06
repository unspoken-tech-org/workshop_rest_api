package com.tproject.workshop.service.auth;

import com.tproject.workshop.dto.auth.RefreshTokenRequest;
import com.tproject.workshop.dto.auth.RefreshTokenResponse;
import com.tproject.workshop.dto.auth.TokenRequest;
import com.tproject.workshop.dto.auth.TokenResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

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
     * @param apiKey  the API Key
     * @param request device/client data
     * @return access token + refresh token
     */
    public TokenResponse authenticate(String apiKey, TokenRequest request) {
        log.info("Token request for deviceId: {}", request.deviceId());

        // Validate the API Key
        apiKeyService.validateApiKey(apiKey);

        // Get client information
        String clientId = apiKeyService.getClientId(apiKey);
        String platform = apiKeyService.getPlatform(apiKey).name();

        // Generate JWT access token
        Map<String, Object> claims = Map.of(
                "platform", platform,
                "deviceId", request.deviceId(),
                "appVersion", request.appVersion()
        );
        String accessToken = jwtService.generateAccessToken(clientId, claims);

        // Generate refresh token
        String refreshToken = refreshTokenService.createRefreshToken(clientId, request.deviceId());

        log.info("Token generated successfully for clientId: {}", clientId);

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

        // Validate refresh token and get clientId
        String clientId = refreshTokenService.validateRefreshToken(request.refreshToken());

        // Generate new access token
        String accessToken = jwtService.generateAccessToken(clientId, Map.of());

        log.debug("Access token renewed for clientId: {}", clientId);

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
}
