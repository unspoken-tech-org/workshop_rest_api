package com.tproject.workshop.dto.brand;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record CreateBrandRequest(
    @NotBlank(message = "Marca é obrigatória")
    @Schema(example = "LG Pro")
    String brand
) {}
