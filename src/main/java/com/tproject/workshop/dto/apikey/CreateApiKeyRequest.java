package com.tproject.workshop.dto.apikey;

import com.tproject.workshop.model.Platform;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record CreateApiKeyRequest(
    @NotBlank(message = "Nome do cliente é obrigatório")
    @Size(max = 100, message = "Nome do cliente deve ter no máximo 100 caracteres")
    @Schema(description = "Nome do cliente", example = "Oficina do João")
    String clientName,

    @NotNull(message = "Plataforma é obrigatória")
    @Schema(description = "Plataforma para a qual a chave será gerada", allowableValues = {"MOBILE", "WEB", "DESKTOP", "SERVER"})
    Platform platform,

    @Size(max = 255, message = "Descrição deve ter no máximo 255 caracteres")
    @Schema(description = "Descrição opcional da finalidade da chave", example = "Chave para o aplicativo Android")
    String description,

    @Schema(description = "Data de expiração da chave (opcional)", example = "2025-12-31T23:59:59Z")
    Instant expiresAt
) {}
