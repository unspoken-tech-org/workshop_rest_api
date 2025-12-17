package com.tproject.workshop.dto.typesBrandsModels;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record BrandRecord(
        @Schema(description = "Unique identifier of the brand", example = "10")
        Integer idBrand,
        @Schema(description = "Brand name", example = "Samsung")
        String brand,
        @Schema(description = "Models offered by the brand", example = "[{\"idModel\":100,\"model\":\"SM-L22\"}]")
        List<ModelRecord> models
) {
}
