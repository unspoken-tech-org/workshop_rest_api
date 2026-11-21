package com.tproject.workshop.dto.auth;

public record RefreshTokenResponse(
        String accessToken, String refreshToken, String tokenType, long expiresIn) {}
