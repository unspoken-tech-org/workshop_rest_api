package com.tproject.workshop.dto.typesBrandsModels;

import io.swagger.v3.oas.annotations.media.Schema;

public record ModelRecord(
        @Schema(description = "Unique identifier of the model", example = "100")
        Integer idModel,
        @Schema(description = "Model name", example = "iPhone 15")
        String model
) {
}
