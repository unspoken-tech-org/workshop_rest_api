package com.tproject.workshop.dto.type;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record CreateTypeRequest(
    @NotBlank(message = "Tipo é obrigatório")
    @Schema(example = "Lavadora Industrial")
    String type
) {}
