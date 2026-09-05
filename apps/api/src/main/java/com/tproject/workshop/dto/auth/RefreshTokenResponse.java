package com.tproject.workshop.dto.auth;

public record RefreshTokenResponse(
    String accessToken,
    String tokenType,
    long expiresIn
) {}
