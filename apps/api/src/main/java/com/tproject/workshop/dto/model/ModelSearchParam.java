package com.tproject.workshop.dto.model;

import com.tproject.workshop.validation.PageBounds;
import com.tproject.workshop.validation.ValidPageNumber;
import com.tproject.workshop.validation.ValidPageSize;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ModelSearchParam(
        @Schema(description = "Type ID to filter models", example = "1")
                @NotNull(message = "O ID do tipo é obrigatório")
                Integer typeId,
        @Schema(description = "Brand ID to filter models", example = "10")
                @NotNull(message = "O ID da marca é obrigatório")
                Integer brandId,
        @Schema(description = "Fuzzy search query on model name", example = "wt123")
                @Size(max = 200, message = "A query de busca deve ter no máximo 200 caracteres")
                String query,
        @Schema(description = "Page number (0-based)", example = "0") @ValidPageNumber Integer page,
        @Schema(description = "Page size", example = "15") @ValidPageSize Integer size) {
    public ModelSearchParam {
        if (query != null && query.isBlank()) query = null;
        page = PageBounds.normalizePage(page);
        size = PageBounds.normalizeSize(size);
    }
}
