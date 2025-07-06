package com.tproject.workshop.dto.device;

import jakarta.validation.constraints.NotBlank;

public record TypeInputDto(
        Integer idType,
        @NotBlank(message = "O tipo do aparelho é obrigatório")
        String type
) {
}
