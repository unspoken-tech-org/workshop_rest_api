package com.tproject.workshop.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Objeto de requisição para renovação do token de acesso")
public record RefreshTokenRequest(
        @NotBlank(message = "refreshToken é obrigatório")
                @Size(max = 256, message = "refreshToken deve ter no máximo 256 caracteres")
                @Schema(
                        description = "Token de renovação (refresh token) obtido anteriormente",
                        example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                        maxLength = 256,
                        requiredMode = Schema.RequiredMode.REQUIRED)
                String refreshToken) {}
