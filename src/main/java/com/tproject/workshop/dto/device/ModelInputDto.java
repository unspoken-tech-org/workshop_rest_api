package com.tproject.workshop.dto.device;

import jakarta.validation.constraints.NotBlank;

public record ModelInputDto(
        Integer idModel,
        @NotBlank(message = "O modelo do aparelho é obrigatório")
        String model
) {
}
