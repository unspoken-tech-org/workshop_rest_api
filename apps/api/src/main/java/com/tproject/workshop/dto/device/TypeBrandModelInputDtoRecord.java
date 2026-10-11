package com.tproject.workshop.dto.device;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record TypeBrandModelInputDtoRecord(
        @Schema(description = "Device type name", example = "DishWasher")
        @NotBlank(message = "O tipo do aparelho é obrigatório")
        String type,
        @Schema(description = "Device brand name", example = "Samsung")
        @NotBlank(message = "A marca do aparelho é obrigatória")
        String brand,
        @Schema(description = "Device model name", example = "SM-DW22")
        @NotBlank(message = "O modelo do aparelho é obrigatório")
        String model
) {
}
