package com.tproject.workshop.dto.device;

import jakarta.validation.constraints.NotNull;

public record TypeBrandModelInputDtoRecord(
        @NotNull(message = "O tipo do aparelho é obrigatório")
        TypeInputDto type,
        @NotNull(message = "A marca do aparelho é obrigatória")
        BrandInputDto brand,
        @NotNull(message = "O modelo do aparelho é obrigatório")
        ModelInputDto model
) {
}

