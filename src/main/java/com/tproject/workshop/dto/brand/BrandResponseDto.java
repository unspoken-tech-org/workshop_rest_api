package com.tproject.workshop.dto.brand;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Brand details response")
public record BrandResponseDto(
    @Schema(example = "1")
    Integer idBrand,
    
    @Schema(example = "Samsung")
    String brand
) {}
