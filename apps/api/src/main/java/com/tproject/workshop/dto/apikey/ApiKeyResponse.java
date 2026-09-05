package com.tproject.workshop.dto.apikey;

import com.tproject.workshop.model.ApiKey;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

public record ApiKeyResponse(
    @Schema(description = "Internal unique identifier")
    Long id,
    
    @Schema(description = "Masked key value for display", example = "sk_mobile_****abcd")
    String maskedKeyValue,
    
    @Schema(description = "Client name (Company)")
    String clientName,

    @Schema(description = "User who owns the key")
    String userIdentifier,
    
    @Schema(description = "Platform associated with the key")
    String platform,
    
    @Schema(description = "Access role")
    String role,
    
    @Schema(description = "Purpose of the key")
    String description,
    
    @Schema(description = "Whether the key is active")
    boolean active,
    
    @Schema(description = "Creation timestamp")
    Instant createdAt,
    
    @Schema(description = "Expiration timestamp")
    Instant expiresAt,
    
    @Schema(description = "Last time the key was used")
    Instant lastUsedAt
) {
    public static ApiKeyResponse fromEntity(ApiKey apiKey) {
        return new ApiKeyResponse(
            apiKey.getId(),
            maskKeyValue(apiKey.getKeyValue()),
            apiKey.getClientName(),
            apiKey.getUserIdentifier(),
            apiKey.getPlatform().name(),
            apiKey.getRole().name(),
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
        int firstUnderscore = keyValue.indexOf('_');
        int secondUnderscore = keyValue.indexOf('_', firstUnderscore + 1);
        String prefix = keyValue.substring(0, secondUnderscore + 1);
        String suffix = keyValue.substring(keyValue.length() - 4);
        return prefix + "****" + suffix;
    }
}