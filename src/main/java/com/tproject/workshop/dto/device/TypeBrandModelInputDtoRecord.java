package com.tproject.workshop.dto.device;

import jakarta.validation.constraints.NotBlank;

public record TypeBrandModelInputDtoRecord(
        @NotBlank(message = "O tipo do aparelho é obrigatório")
        String type,
        @NotBlank(message = "A marca do aparelho é obrigatória")
        String brand,
        @NotBlank(message = "O modelo do aparelho é obrigatório")
        String model
) {
}

