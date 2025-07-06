package com.tproject.workshop.dto.device;

import jakarta.validation.constraints.NotBlank;

public record BrandInputDto(
        Integer idBrand,
        @NotBlank(message = "A marca do aparelho é obrigatória")
        String brand
) {
}
