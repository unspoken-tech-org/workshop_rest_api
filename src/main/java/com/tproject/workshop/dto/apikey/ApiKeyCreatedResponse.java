package com.tproject.workshop.dto.apikey;

import com.tproject.workshop.model.ApiKey;
import com.tproject.workshop.model.Platform;
import com.tproject.workshop.model.Role;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * Response returned only when creating a new API Key.
 * Contains the full keyValue which is only shown once at creation time.
 */
public record ApiKeyCreatedResponse(
    @Schema(description = "Internal unique identifier")
    Long id,

    @Schema(description = "Full API Key value - STORE THIS SECURELY, it will not be shown again", example = "sk_mobile_abc123def456")
    String keyValue,

    @Schema(description = "Client name (Company)")
    String clientName,

    @Schema(description = "User who owns the key")
    String userIdentifier,

    @Schema(description = "Target platform")
    Platform platform,

    @Schema(description = "Access role")
    Role role,

    @Schema(description = "Optional description")
    String description,

    @Schema(description = "Creation timestamp")
    Instant createdAt,

    @Schema(description = "Expiration timestamp")
    Instant expiresAt
) {
    public static ApiKeyCreatedResponse fromEntity(ApiKey apiKey) {
        return new ApiKeyCreatedResponse(
            apiKey.getId(),
            apiKey.getKeyValue(),
            apiKey.getClientName(),
            apiKey.getUserIdentifier(),
            apiKey.getPlatform(),
            apiKey.getRole(),
            apiKey.getDescription(),
            apiKey.getCreatedAt(),
            apiKey.getExpiresAt()
        );
    }
}