package com.tproject.workshop.dto.type;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record TypeSearchParam(
    @Schema(description = "Fuzzy search query on type name", example = "lav")
    @Size(max = 200, message = "A query de busca deve ter no máximo 200 caracteres")
    String query,

    @Schema(description = "Page number (0-based)", example = "0")
    @Min(value = 0, message = "O numero da pagina deve ser maior ou igual a zero")
    Integer page,

    @Schema(description = "Page size", example = "15")
    @Min(value = 1, message = "O tamanho da pagina deve ser maior que zero")
    @Max(value = 100, message = "O tamanho da pagina deve ser no máximo 100")
    Integer size
) {
    public TypeSearchParam {
        if (query != null && query.isBlank()) query = null;
        if (page == null) page = 0;
        if (size == null) size = 15;
    }
}
