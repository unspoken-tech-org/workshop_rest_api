package com.tproject.workshop.dto.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record CreateModelRequest(
    @NotBlank(message = "Modelo é obrigatório")
    @Schema(example = "WT500X")
    String model
) {}
