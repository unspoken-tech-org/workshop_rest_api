package com.tproject.workshop.controller;

import com.tproject.workshop.config.openapi.ApiGlobalResponses;
import com.tproject.workshop.dto.auth.RefreshTokenRequest;
import com.tproject.workshop.dto.auth.RefreshTokenResponse;
import com.tproject.workshop.dto.auth.TokenRequest;
import com.tproject.workshop.dto.auth.TokenResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Authentication", description = "JWT authentication endpoints")
public interface AuthController {

    @Operation(
            summary = "Get tokens",
            description = "Exchange a valid API Key for JWT access and refresh tokens. " +
                    "The API Key must be sent in the X-API-Key header.")
    @ApiGlobalResponses
    @ApiResponse(responseCode = "200", description = "Tokens generated successfully")
    @PostMapping("/token")
    TokenResponse getToken(
            @Parameter(description = "Valid API Key") @RequestHeader("X-API-Key") String apiKey,
            @Valid @RequestBody TokenRequest request);

    @Operation(
            summary = "Refresh token",
            description = "Renew the access token using a valid refresh token.")
    @ApiGlobalResponses
    @ApiResponse(responseCode = "200", description = "Access token renewed successfully")
    @PostMapping("/refresh")
    RefreshTokenResponse refreshToken(@Valid @RequestBody RefreshTokenRequest request);

    @Operation(
            summary = "Revoke token",
            description = "Revoke a refresh token (logout). The token will no longer be usable.")
    @ApiGlobalResponses
    @ApiResponse(responseCode = "204", description = "Token revoked successfully")
    @PostMapping("/revoke")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void revokeToken(@Valid @RequestBody RefreshTokenRequest request);
}
