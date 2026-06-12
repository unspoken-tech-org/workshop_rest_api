package com.tproject.workshop.dto.brand;

import io.swagger.v3.oas.annotations.media.Schema;

public record CreateBrandRequest(
    @Schema(example = "LG Pro")
    String brand
) {}
