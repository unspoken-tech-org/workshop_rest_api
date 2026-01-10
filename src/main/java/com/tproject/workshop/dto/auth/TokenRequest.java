package com.tproject.workshop.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Objeto de requisição para geração de token de acesso")
public record TokenRequest(
    @NotBlank(message = "deviceId é obrigatório")
    @Schema(description = "Identificador único do dispositivo", example = "35245100-ab32-4fd1-9b04-3d59cf56578a", requiredMode = Schema.RequiredMode.REQUIRED)
    String deviceId,
    
    @NotBlank(message = "appVersion é obrigatório")
    @Schema(description = "Versão atual do aplicativo", example = "1.0.5", requiredMode = Schema.RequiredMode.REQUIRED)
    String appVersion
) {}
