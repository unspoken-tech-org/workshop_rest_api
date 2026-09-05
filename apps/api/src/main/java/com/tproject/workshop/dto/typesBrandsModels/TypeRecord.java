package com.tproject.workshop.dto.typesBrandsModels;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record TypeRecord(
        @Schema(description = "Unique identifier of the device type", example = "1")
        Integer idType,
        @Schema(description = "Name of the device type", example = "DishWasher")
        String type,
        @Schema(description = "Brands that belong to this type", example = "[{\"idBrand\":10,\"brand\":\"Samsung\"}]")
        List<BrandRecord> brands
) {
}
