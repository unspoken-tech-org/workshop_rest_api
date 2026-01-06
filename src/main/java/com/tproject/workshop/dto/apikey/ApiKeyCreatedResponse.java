package com.tproject.workshop.dto.apikey;

import com.tproject.workshop.model.ApiKey;
import com.tproject.workshop.model.Platform;

import java.time.Instant;

/**
 * Response returned only when creating a new API Key.
 * Contains the full keyValue which is only shown once at creation time.
 */
public record ApiKeyCreatedResponse(
    Long id,
    String keyValue,
    String clientName,
    Platform platform,
    String description,
    Instant createdAt,
    Instant expiresAt
) {
    public static ApiKeyCreatedResponse fromEntity(ApiKey apiKey) {
        return new ApiKeyCreatedResponse(
            apiKey.getId(),
            apiKey.getKeyValue(),
            apiKey.getClientName(),
            apiKey.getPlatform(),
            apiKey.getDescription(),
            apiKey.getCreatedAt(),
            apiKey.getExpiresAt()
        );
    }
}
