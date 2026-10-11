package com.tproject.workshop.dto.apikey;

import com.tproject.workshop.model.Platform;
import com.tproject.workshop.model.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record CreateApiKeyRequest(
    @NotBlank(message = "Nome do cliente é obrigatório")
    @Size(max = 100, message = "Nome do cliente deve ter no máximo 100 caracteres")
    @Schema(description = "Client name (Company/Tenant)", example = "John's Workshop")
    String clientName,

    @NotBlank(message = "Identificador do usuário é obrigatório")
    @Size(max = 100, message = "Identificador do usuário deve ter no máximo 100 caracteres")
    @Schema(description = "User identifier (Technician/Device)", example = "tech_jose")
    String userIdentifier,

    @NotNull(message = "Plataforma é obrigatória")
    @Schema(description = "Target platform for the API key", allowableValues = {"MOBILE", "WEB", "DESKTOP", "SERVER"})
    Platform platform,

    @Schema(description = "Access level for the key", allowableValues = {"ADMIN", "SERVICE"}, defaultValue = "SERVICE")
    Role role,

    @Size(max = 255, message = "Descrição deve ter no máximo 255 caracteres")
    @Schema(description = "Optional description of the key purpose", example = "Key for Android app")
    String description,

    @Schema(description = "Optional key expiration date", example = "2025-12-31T23:59:59Z")
    Instant expiresAt
) {}