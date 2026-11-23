package com.tproject.workshop.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Objeto de requisição para geração de token de acesso")
public record TokenRequest(
        @NotBlank(message = "boundDeviceId é obrigatório")
                @Size(max = 100, message = "boundDeviceId deve ter no máximo 100 caracteres")
                @Schema(
                        description = "Identificador único do dispositivo vinculado",
                        example = "35245100-ab32-4fd1-9b04-3d59cf56578a",
                        maxLength = 100,
                        requiredMode = Schema.RequiredMode.REQUIRED)
                String boundDeviceId,
        @NotBlank(message = "appVersion é obrigatório")
                @Size(max = 50, message = "appVersion deve ter no máximo 50 caracteres")
                @Schema(
                        description = "Versão atual do aplicativo",
                        example = "1.0.5",
                        maxLength = 50,
                        requiredMode = Schema.RequiredMode.REQUIRED)
                String appVersion) {}
