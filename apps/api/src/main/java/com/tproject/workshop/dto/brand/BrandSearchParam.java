package com.tproject.workshop.dto.brand;

import com.tproject.workshop.validation.PageBounds;
import com.tproject.workshop.validation.ValidPageNumber;
import com.tproject.workshop.validation.ValidPageSize;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

public record BrandSearchParam(
        @Schema(description = "Fuzzy search query on brand name", example = "lg")
                @Size(max = 200, message = "A query de busca deve ter no máximo 200 caracteres")
                String query,
        @Schema(description = "Page number (0-based)", example = "0") @ValidPageNumber Integer page,
        @Schema(description = "Page size", example = "15") @ValidPageSize Integer size) {
    public BrandSearchParam {
        if (query != null && query.isBlank()) query = null;
        page = PageBounds.normalizePage(page);
        size = PageBounds.normalizeSize(size);
    }
}
