package com.tproject.workshop.dto.brand;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;

public record BrandSearchParam(
    @Schema(description = "Fuzzy search query on brand name", example = "lg")
    String query,

    @Schema(description = "Page number (0-based)", example = "0")
    @Min(value = 0, message = "O numero da pagina deve ser maior ou igual a zero")
    Integer page,

    @Schema(description = "Page size", example = "15")
    @Min(value = 1, message = "O tamanho da pagina deve ser maior que zero")
    Integer size
) {
    public BrandSearchParam {
        if (query != null && query.isBlank()) query = null;
        if (page == null) page = 0;
        if (size == null) size = 15;
    }
}
