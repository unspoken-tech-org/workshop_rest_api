package com.tproject.workshop.dto.apikey;

import com.tproject.workshop.model.ApiKey;
import com.tproject.workshop.model.Platform;

import java.time.Instant;

public record ApiKeyResponse(
    Long id,
    String maskedKeyValue,
    String clientName,
    Platform platform,
    String description,
    boolean active,
    Instant createdAt,
    Instant expiresAt,
    Instant lastUsedAt
) {
    public static ApiKeyResponse fromEntity(ApiKey apiKey) {
        return new ApiKeyResponse(
            apiKey.getId(),
            maskKeyValue(apiKey.getKeyValue()),
            apiKey.getClientName(),
            apiKey.getPlatform(),
            apiKey.getDescription(),
            apiKey.isActive(),
            apiKey.getCreatedAt(),
            apiKey.getExpiresAt(),
            apiKey.getLastUsedAt()
        );
    }

    private static String maskKeyValue(String keyValue) {
        if (keyValue == null || keyValue.length() < 20) {
            return "****";
        }
        // Show prefix and last 4 characters: sk_mobile_****abcd
        String prefix = keyValue.substring(0, keyValue.indexOf('_', 3) + 1);
        String suffix = keyValue.substring(keyValue.length() - 4);
        return prefix + "****" + suffix;
    }
}
